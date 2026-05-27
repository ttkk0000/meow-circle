#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import re
import sys
from html.parser import HTMLParser
from urllib.parse import urlparse

# Pages that must be verified
SMOKE_PAGES = [
    "index.html",
    "discover.html",
    "compose.html",
    "market.html",
    "messages.html",
    "profile.html",
    "login.html",
    "register.html",
    "dashboard.html",
    "admin.html",
    "post.html",
    "pawpop-mobile.html",
    "pawpop-desktop.html",
    "cute.html"
]

# Forbidden visible product terms
FORBIDDEN_TERMS = [
    "喵友圈",
    "Kitty Circle",
    "Meow Circle",
    "铲屎官",
    "布偶猫交流群"
]

# Case-insensitive term to check, except when referring to local filenames/code
FORBIDDEN_PAWPOP = re.compile(r'\bpawpop\b', re.IGNORECASE)

class SmokeTestParser(HTMLParser):
    def __init__(self, filepath, web_dir):
        super().__init__()
        self.filepath = filepath
        self.web_dir = web_dir
        self.errors = []
        self.warnings = []
        self.links_to_check = []
        self.in_script_or_style = False

    def handle_starttag(self, tag, attrs):
        if tag in ["script", "style"]:
            self.in_script_or_style = True

        attrs_dict = dict(attrs)

        # Check references: src, href, link
        for attr in ["src", "href"]:
            if attr in attrs_dict:
                val = attrs_dict[attr]
                # Check for forbidden terms in raw attributes if they aren't obviously filename/code references
                for term in FORBIDDEN_TERMS:
                    if term in val:
                        self.errors.append(f"Forbidden term '{term}' found in attribute '{attr}={val}'")
                
                # Link checks
                self.check_link_reference(tag, attr, val)

        # Check title and placeholder attributes for forbidden terms
        for attr in ["title", "placeholder", "data-i18n-placeholder"]:
            if attr in attrs_dict:
                val = attrs_dict[attr]
                for term in FORBIDDEN_TERMS:
                    if term in val:
                        self.errors.append(f"Forbidden term '{term}' found in visible attribute '{attr}={val}'")
                if FORBIDDEN_PAWPOP.search(val):
                    self.errors.append(f"Forbidden term 'Pawpop' found in visible attribute '{attr}={val}'")

    def handle_endtag(self, tag):
        if tag in ["script", "style"]:
            self.in_script_or_style = False

    def handle_data(self, data):
        if self.in_script_or_style:
            return

        # Check visible text for forbidden terms
        text = data.strip()
        if not text:
            return

        for term in FORBIDDEN_TERMS:
            if term in text:
                self.errors.append(f"Forbidden term '{term}' found in visible text: '{text}'")

        if FORBIDDEN_PAWPOP.search(text):
            # Check if it's not a benign comment or file reference
            # Allow text that is just the file name (e.g. pawpop-mobile.html)
            clean_text = re.sub(r'[\w\-]+\.(html|css|js|png|jpg|jpeg)', '', text, flags=re.IGNORECASE).strip()
            if FORBIDDEN_PAWPOP.search(clean_text):
                self.errors.append(f"Forbidden term 'Pawpop' found in visible text: '{text}'")

    def check_link_reference(self, tag, attr, val):
        # Ignore external links, mailto, tel, empty links, or hash-only links
        if not val or val.startswith(('#', 'http://', 'https://', 'data:', 'mailto:', 'tel:')):
            return

        # Parse URL path
        parsed = urlparse(val)
        path = parsed.path
        if not path:
            return

        # If it's a root-relative path (e.g. /shared.js), strip leading slash
        if path.startswith('/'):
            target_rel = path[1:]
        else:
            # Relative path relative to the current HTML file directory
            # For our flat web directory, files are in the same folder
            target_rel = path

        # Resolve path in web_dir
        target_abs = os.path.join(self.web_dir, target_rel)

        # Normalize path
        target_abs = os.path.normpath(target_abs)

        # Check if file exists. If it doesn't have an extension, try appending .html
        exists = os.path.exists(target_abs)
        if not exists and not os.path.splitext(target_abs)[1]:
            # Try with .html extension (mirrors Go backend routing for e.g. /login, /register, /dashboard)
            exists = os.path.exists(target_abs + ".html")

        if not exists:
            self.errors.append(f"Broken reference: tag <{tag}> with {attr}='{val}' refers to missing file/route '{target_rel}'")


def verify_page(filename, web_dir):
    filepath = os.path.join(web_dir, filename)
    print(f"Verifying {filename}... ", end="")

    if not os.path.exists(filepath):
        print("FAIL (File does not exist)")
        return False, [f"File {filename} is missing from web directory"]

    try:
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
    except Exception as e:
        print(f"FAIL (Could not read file: {e})")
        return False, [f"Could not read {filename}: {e}"]

    parser = SmokeTestParser(filepath, web_dir)
    try:
        parser.feed(content)
    except Exception as e:
        print(f"FAIL (HTML Parse Error: {e})")
        return False, [f"HTML parse error in {filename}: {e}"]

    if parser.errors:
        print(f"FAIL ({len(parser.errors)} errors)")
        for err in parser.errors:
            print(f"  - Error: {err}")
        return False, parser.errors
    else:
        print("OK")
        return True, []


def main():
    # Resolve scripts directory and project root
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    web_dir = os.path.join(project_root, "web")

    print("==================================================")
    print(" M&D Web UI Automated Smoke Test & Verification   ")
    print("==================================================")
    print(f"Project root: {project_root}")
    print(f"Web directory: {web_dir}\n")

    if not os.path.exists(web_dir):
        print(f"Error: Web directory '{web_dir}' does not exist.")
        sys.exit(1)

    total_errors = 0
    passed_pages = 0

    for page in SMOKE_PAGES:
        passed, errors = verify_page(page, web_dir)
        if passed:
            passed_pages += 1
        else:
            total_errors += len(errors)

    print("\n==================================================")
    print(f"Results: {passed_pages}/{len(SMOKE_PAGES)} pages passed.")
    if total_errors > 0:
        print(f"Total verification failures: {total_errors}")
        print("Please fix the highlighted errors before committing.")
        sys.exit(1)
    else:
        print("All static pages successfully verified! Brand naming and assets are aligned.")
        sys.exit(0)

if __name__ == "__main__":
    main()
