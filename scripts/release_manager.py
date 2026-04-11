import subprocess
import re
import sys
import json
import argparse
from datetime import datetime

def run_command(command):
    try:
        result = subprocess.run(command, shell=True, check=True, capture_output=True, text=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        if "git describe" not in command:
            print(f"Error running command: {command}\n{e.stderr}")
        return None

def get_last_tag_info():
    tag = run_command("git describe --tags --abbrev=0")
    if not tag:
        tag = run_command("git rev-list --max-parents=0 HEAD")
        date = run_command(f"git log -1 --format=%aI {tag}")
        return tag, date
    
    date = run_command(f"git log -1 --format=%aI {tag}")
    return tag, date

def get_valid_closed_issues(last_tag_date_str):
    pr_json = run_command("gh pr list --state merged --limit 50 --json number,mergedAt,title,body,closingIssuesReferences")
    if not pr_json:
        return []
        
    try:
        all_merged_prs = json.loads(pr_json)
    except:
        return []

    limit_date = datetime.fromisoformat(last_tag_date_str.replace('Z', '+00:00'))
    
    # 1. Fetch Merged PRs and their linked issues
    pr_json = run_command("gh pr list --state merged --limit 50 --json number,mergedAt,title,body,closingIssuesReferences")
    issue_to_pr = {}
    if pr_json:
        try:
            all_merged_prs = json.loads(pr_json)
            for pr in all_merged_prs:
                merged_at_str = pr.get('mergedAt')
                if not merged_at_str: continue
                pr_date = datetime.fromisoformat(merged_at_str.replace('Z', '+00:00'))
                if pr_date <= limit_date: continue
                
                pr_num = str(pr['number'])
                for ref in pr.get('closingIssuesReferences', []):
                    issue_to_pr[str(ref.get('number'))] = pr_num
                
                body_text = (pr.get('title', "") + " " + pr.get('body', "")).upper()
                # Mirroring AK instead of PP
                mentions = re.findall(r'(?:#|AK-)(\d+)', body_text)
                for num in mentions:
                    if num not in issue_to_pr:
                        issue_to_pr[num] = pr_num
        except: pass

    # 2. Fetch all Closed Issues since the limit date to catch direct pushes
    issue_json = run_command("gh issue list --state closed --limit 50 --json number,title,closedAt,labels")
    direct_issues = []
    if issue_json:
        try:
            all_closed = json.loads(issue_json)
            for iss in all_closed:
                closed_at_str = iss.get('closedAt')
                if not closed_at_str: continue
                issue_date = datetime.fromisoformat(closed_at_str.replace('Z', '+00:00'))
                if issue_date > limit_date:
                    direct_issues.append(iss)
        except: pass

    # 3. Combine them
    valid_issues_map = {}
    
    # Add PR-linked ones
    for issue_num, pr_num in issue_to_pr.items():
        # Re-fetch data to get labels and closed date
        iss_data_json = run_command(f"gh issue view {issue_num} --json number,title,closedAt,state,labels")
        if not iss_data_json: continue
        iss_data = json.loads(iss_data_json)
        if iss_data.get('state') == 'CLOSED':
            closed_at_str = iss_data.get('closedAt')
            if closed_at_str:
                issue_date = datetime.fromisoformat(closed_at_str.replace('Z', '+00:00'))
                if issue_date > limit_date:
                    valid_issues_map[str(iss_data['number'])] = {
                        "number": str(iss_data['number']),
                        "title": iss_data.get('title'),
                        "pr": pr_num,
                        "labels": [l['name'] for l in iss_data.get('labels', [])]
                    }

    # Add direct-closed ones (if not already added via PR)
    for iss in direct_issues:
        num_str = str(iss['number'])
        if num_str not in valid_issues_map:
            valid_issues_map[num_str] = {
                "number": num_str,
                "title": iss.get('title'),
                "pr": None,
                "labels": [l['name'] for l in iss.get('labels', [])]
            }
                
    return list(valid_issues_map.values())

def check_open_blockers():
    # Only block if there are OPEN high/critical priority issues
    blockers = run_command("gh issue list --label \"priority:high\" --label \"priority:critical\" --limit 10 --json number,title")
    if not blockers or not blockers.strip():
        return []
    try:
        return json.loads(blockers)
    except:
        return []

def get_current_version_from_file():
    gradle_path = "app/build.gradle.kts"
    with open(gradle_path, 'r') as f:
        content = f.read()
    version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    return version_name_match.group(1) if version_name_match else None

def update_version(issues):
    gradle_path = "app/build.gradle.kts"
    with open(gradle_path, 'r') as f:
        content = f.read()

    version_code_match = re.search(r'versionCode\s*=\s*(\d+)', content)
    if not version_code_match:
        print("Could not find versionCode in build.gradle.kts")
        sys.exit(1)
    
    current_code = int(version_code_match.group(1))
    new_code = current_code + 1
    
    version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    if not version_name_match:
        print("Could not find versionName in build.gradle.kts")
        sys.exit(1)
    
    current_name = version_name_match.group(1)
    parts = current_name.split('.')
    if len(parts) < 3:
        while len(parts) < 3:
            parts.append('0')
    
    major = int(parts[0])
    minor = int(parts[1])
    patch = int(parts[2])

    # Determine Bump Type
    is_feature = any("type:feature" in iss['labels'] or "enhancement" in iss['labels'] for iss in issues)
    
    PATCH_LIMIT = 9 # If we reach .9, the next fix bump becomes a Minor "milestone"
    
    if is_feature:
        minor += 1
        patch = 0
        print(f"Detected Feature(s) - Bumping Minor Version to {major}.{minor}.{patch}")
    elif patch >= PATCH_LIMIT:
        minor += 1
        patch = 0
        print(f"Reached Patch Limit ({PATCH_LIMIT}) - Bumping Minor Version as a Milestone to {major}.{minor}.{patch}")
    else:
        patch += 1
        print(f"Bug fixes only - Bumping Patch Version to {major}.{minor}.{patch}")
    
    new_name = f"{major}.{minor}.{patch}"
    
    content = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {new_code}', content)
    content = re.sub(r'versionName\s*=\s*"[^"]+"', f'versionName = "{new_name}"', content)
    
    with open(gradle_path, 'w') as f:
        f.write(content)
    
    return current_name, new_name

def update_changelog(new_version, issues):
    changelog_path = "CHANGELOG.md"
    date_str = datetime.now().strftime("%Y-%m-%d")
    header = f"## [{new_version}] - {date_str}\n\n### Fixed / Changed\n\n"
    
    lines = []
    sorted_issues = sorted(issues, key=lambda x: int(x['number']))
    for issue in sorted_issues:
        lines.append(f"- {issue['title']} (Issue #{issue['number']}, via PR #{issue['pr']})")
    
    new_entry = header + "\n".join(lines) + "\n\n"
    
    # Save a copy for the GitHub Release Body
    with open("latest_changelog.txt", 'w') as f:
        f.write(new_entry)
        
    with open(changelog_path, 'r') as f:
        current_content = f.read()
    
    marker = "# Changelog\n\n"
    if marker in current_content:
        updated_content = current_content.replace(marker, marker + new_entry)
    else:
        updated_content = marker + new_entry + current_content
    
    with open(changelog_path, 'w') as f:
        f.write(updated_content)

def generate_whatsnew(issues):
    import os
    output_dir = "distribution/whatsnew"
    os.makedirs(output_dir, exist_ok=True)
    
    lines = []
    for issue in issues:
        title = issue['title']
        # Sanitize: Remove (Issue #XX) or [Fix: ...] tags
        title = re.sub(r'\(Issue\s*#\d+.*?\)', '', title, flags=re.IGNORECASE)
        title = re.sub(r'via\s*PR\s*#\d+', '', title, flags=re.IGNORECASE)
        title = title.strip()
        lines.append(f"- {title}")
    
    content = "\n".join(lines)
    # Play Store Limit is 500 characters
    if len(content) > 500:
        content = content[:497] + "..."
        
    with open(os.path.join(output_dir, "whatsnew-en-US"), 'w') as f:
        f.write(content)

def main():
    parser = argparse.ArgumentParser(description="Arrow Kotlin Release Manager")
    parser.add_argument("--check", action="store_true", help="Only check if release criteria are met")
    parser.add_argument("--bump", action="store_true", help="Locally bump version and update changelog (no commit)")
    parser.add_argument("--finalize", action="store_true", help="Commit the changes and create a git tag")
    args = parser.parse_args()

    tag, date = get_last_tag_info()
    issues = get_valid_closed_issues(date)
    open_blockers = check_open_blockers()
    
    # Determine Dynamic Threshold
    is_hotfix = any("priority:critical" in iss['labels'] for iss in issues)
    THRESHOLD = 1 if is_hotfix else 4
    
    met = len(issues) >= THRESHOLD and len(open_blockers) == 0
    
    if args.check:
        print(f"Scanning from release {tag} (dated {date})")
        if is_hotfix:
            print("(!) CRITICAL HOTFIX DETECTED - Threshold set to 1.")
        else:
            print(f"Regular release mode - Threshold set to {THRESHOLD}.")
            
        print(f"Verified {len(issues)} PR-linked closed issues.")
        for iss in issues:
            label_str = f" [{', '.join(iss['labels'])}]" if iss['labels'] else ""
            print(f"  - [#{iss['number']}] {iss['title']} (via PR #{iss['pr']}){label_str}")
            
        if open_blockers:
            print(f"[-] Open high/critical blockers: {len(open_blockers)}")
            for b in open_blockers:
                print(f"  - [#{b['number']}] {b['title']}")
        
        if met:
            print(f"[*] Release conditions met ({len(issues)}/{THRESHOLD}).")
            sys.exit(0)
        else:
            print(f"[-] Release criteria not met yet ({len(issues)}/{THRESHOLD}).")
            sys.exit(1)

    if args.bump:
        if not met:
            print("Error: Criteria not met. Cannot bump version.")
            sys.exit(1)
        print("Bumping version and updating CHANGELOG.md locally...")
        old_v, new_v = update_version(issues)
        update_changelog(new_v, issues)
        generate_whatsnew(issues) # Sync Play Store notes
        print(f"[OK] Locally updated to v{new_v}. (Ready for build)")
        sys.exit(0)
            
    if args.finalize:
        current_v = get_current_version_from_file()
        if not current_v:
            print("Error: Could not determine current version.")
            sys.exit(1)
            
        print(f"Committing and tagging release v{current_v}...")
        run_command("git add app/build.gradle.kts CHANGELOG.md")
        run_command(f'git commit -m "chore(release): v{current_v} release"')
        run_command(f'git tag -a v{current_v} -m "Release v{current_v}"')
        print(f"[OK] Finalized release v{current_v} on GitHub.")
        sys.exit(0)

if __name__ == "__main__":
    main()
