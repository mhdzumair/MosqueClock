#!/bin/bash

# MosqueClock - Automated GitHub Release Script
# Builds the 'github' flavor with self-update feature enabled
# Handles version bumping, building, tagging, and GitHub release creation
#
# Note: This script builds the 'github' variant (with self-update feature)
#       For Play Store releases, use: ./gradlew bundlePlaystoreRelease
#
# Usage:
#   ./release.sh                    - Full automated release
#   ./release.sh --build-only       - Build APK only (no git/GitHub operations)
#   ./release.sh --no-clean         - Skip clean step (faster builds)
#   ./release.sh --build-only --no-clean - Combine flags

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="MosqueClock"
PACKAGE_NAME="com.mosque.prayerclock"
BUILD_GRADLE="app/build.gradle.kts"
OUTPUT_DIR="app/build/outputs/apk/github/release"
APK_NAME="app-github-release.apk"
RELEASE_DIR="release"
GITHUB_REPO="mhdzumair/MosqueClock"

# Parse command line arguments
BUILD_ONLY=false
NO_CLEAN=false

for arg in "$@"; do
    case $arg in
        --build-only)
            BUILD_ONLY=true
            ;;
        --no-clean)
            NO_CLEAN=true
            ;;
        *)
            echo "Unknown option: $arg"
            echo ""
            echo "Usage:"
            echo "  ./release.sh                    - Full automated release"
            echo "  ./release.sh --build-only       - Build APK only (no git/GitHub operations)"
            echo "  ./release.sh --no-clean         - Skip clean step (faster builds)"
            echo "  ./release.sh --build-only --no-clean - Combine flags"
            exit 1
            ;;
    esac
done

# Functions
print_header() {
    echo ""
    echo -e "${CYAN}${BOLD}================================================${NC}"
    echo -e "${CYAN}${BOLD}  $1${NC}"
    echo -e "${CYAN}${BOLD}================================================${NC}"
    echo ""
}

print_step() {
    echo -e "${YELLOW}➜ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check if we're in the project root
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew not found. Are you in the project root?"
        exit 1
    fi
    print_success "Gradle wrapper found"
    
    # Make gradlew executable
    chmod +x ./gradlew
    print_success "Made gradlew executable"
    
    # Skip git/GitHub checks if build-only mode
    if [ "$BUILD_ONLY" = true ]; then
        print_info "Build-only mode: Skipping git/GitHub checks"
        return
    fi
    
    # Check if git is installed
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed"
        exit 1
    fi
    print_success "Git found"
    
    # Check if gh (GitHub CLI) is installed
    if ! command -v gh &> /dev/null; then
        print_error "GitHub CLI (gh) is not installed"
        echo ""
        echo "Install GitHub CLI:"
        echo "  macOS:   brew install gh"
        echo "  Linux:   https://github.com/cli/cli/blob/trunk/docs/install_linux.md"
        echo ""
        exit 1
    fi
    print_success "GitHub CLI found"
    
    # Check if gh is authenticated
    if ! gh auth status &> /dev/null; then
        print_error "GitHub CLI is not authenticated"
        echo ""
        echo "Run: gh auth login"
        echo ""
        exit 1
    fi
    print_success "GitHub CLI authenticated"
    
    # Check for uncommitted changes
    if [ -n "$(git status --porcelain)" ]; then
        print_warning "You have uncommitted changes"
        git status --short
        echo ""
        read -p "Continue anyway? [Y/n] " -r
        if [[ $REPLY =~ ^[Nn]$ ]]; then
            print_info "Release cancelled"
            exit 0
        fi
    else
        print_success "Working directory is clean"
    fi
    
    # Check if on main/master branch
    CURRENT_BRANCH=$(git branch --show-current)
    if [ "$CURRENT_BRANCH" != "main" ] && [ "$CURRENT_BRANCH" != "master" ]; then
        print_warning "Not on main/master branch (current: $CURRENT_BRANCH)"
        read -p "Continue anyway? [Y/n] " -r
        if [[ $REPLY =~ ^[Nn]$ ]]; then
            print_info "Release cancelled"
            exit 0
        fi
    else
        print_success "On $CURRENT_BRANCH branch"
    fi
}

# Get current version from build.gradle.kts
get_current_version() {
    local version_code=$(grep 'versionCode = ' "$BUILD_GRADLE" | sed 's/.*= \(.*\)/\1/' | tr -d ' ')
    local version_name=$(grep 'versionName = ' "$BUILD_GRADLE" | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
    
    CURRENT_VERSION_CODE=$version_code
    CURRENT_VERSION_NAME=$version_name
}

# Prompt for new version
prompt_new_version() {
    print_header "Version Information"
    
    get_current_version
    
    print_info "Current Version Code: $CURRENT_VERSION_CODE"
    print_info "Current Version Name: $CURRENT_VERSION_NAME"
    echo ""
    
    # Parse current version
    IFS='.' read -r -a version_parts <<< "$CURRENT_VERSION_NAME"
    MAJOR=${version_parts[0]:-0}
    MINOR=${version_parts[1]:-0}
    PATCH=${version_parts[2]:-0}
    
    # Calculate suggested versions
    PATCH_VERSION="$MAJOR.$MINOR.$((PATCH + 1))"
    MINOR_VERSION="$MAJOR.$((MINOR + 1)).0"
    MAJOR_VERSION="$((MAJOR + 1)).0.0"
    
    echo -e "${BOLD}Select release type:${NC}"
    echo "  1) Patch   (bug fixes)         → v$PATCH_VERSION"
    echo "  2) Minor   (new features)      → v$MINOR_VERSION"
    echo "  3) Major   (breaking changes)  → v$MAJOR_VERSION"
    echo "  4) Custom  (enter manually)"
    echo ""
    
    read -p "Enter choice (1-4): " -n 1 -r choice
    echo ""
    echo ""
    
    case $choice in
        1)
            NEW_VERSION_NAME=$PATCH_VERSION
            ;;
        2)
            NEW_VERSION_NAME=$MINOR_VERSION
            ;;
        3)
            NEW_VERSION_NAME=$MAJOR_VERSION
            ;;
        4)
            read -p "Enter new version (e.g., 1.2.0): " NEW_VERSION_NAME
            ;;
        *)
            print_error "Invalid choice"
            exit 1
            ;;
    esac
    
    # Auto-increment version code
    NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
    
    echo ""
    print_info "New Version Code: $NEW_VERSION_CODE (auto-incremented)"
    print_info "New Version Name: $NEW_VERSION_NAME"
    echo ""
    
    read -p "Proceed with this version? [Y/n] " -r
    if [[ $REPLY =~ ^[Nn]$ ]]; then
        print_info "Release cancelled"
        exit 0
    fi
}

# Update version in build.gradle.kts
update_version() {
    print_header "Updating Version"
    
    print_step "Updating $BUILD_GRADLE..."
    
    # Backup original file
    cp "$BUILD_GRADLE" "$BUILD_GRADLE.backup"
    
    # Update versionCode
    sed -i.tmp "s/versionCode = $CURRENT_VERSION_CODE/versionCode = $NEW_VERSION_CODE/" "$BUILD_GRADLE"
    
    # Update versionName
    sed -i.tmp "s/versionName = \"$CURRENT_VERSION_NAME\"/versionName = \"$NEW_VERSION_NAME\"/" "$BUILD_GRADLE"
    
    # Remove temporary file
    rm -f "$BUILD_GRADLE.tmp"
    
    # Verify changes
    NEW_CODE_CHECK=$(grep 'versionCode = ' "$BUILD_GRADLE" | sed 's/.*= \(.*\)/\1/' | tr -d ' ')
    NEW_NAME_CHECK=$(grep 'versionName = ' "$BUILD_GRADLE" | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
    
    if [ "$NEW_CODE_CHECK" != "$NEW_VERSION_CODE" ] || [ "$NEW_NAME_CHECK" != "$NEW_VERSION_NAME" ]; then
        print_error "Version update verification failed"
        # Restore backup
        mv "$BUILD_GRADLE.backup" "$BUILD_GRADLE"
        exit 1
    fi
    
    # Remove backup
    rm -f "$BUILD_GRADLE.backup"
    
    print_success "Version updated to v$NEW_VERSION_NAME (code: $NEW_VERSION_CODE)"
}

# Clean previous builds
clean_build() {
    if [ "$NO_CLEAN" = true ]; then
        print_header "Skipping Clean Step"
        print_info "Building incrementally (--no-clean flag)"
        return
    fi
    
    print_header "Cleaning Previous Builds"
    print_step "Removing old build artifacts..."
    
    ./gradlew clean
    
    print_success "Clean completed"
}

# Build release APK
build_release() {
    print_header "Building GitHub Release APK"
    print_step "Building github variant with ProGuard optimization..."
    print_info "This may take a few minutes..."
    echo ""
    
    ./gradlew assembleGithubRelease
    
    print_success "Build completed successfully"
}

# Verify APK
verify_apk() {
    print_header "Verifying APK"
    
    APK_PATH="$OUTPUT_DIR/$APK_NAME"
    
    if [ ! -f "$APK_PATH" ]; then
        print_error "APK not found at $APK_PATH"
        exit 1
    fi
    print_success "APK file exists"
    
    # Get APK size
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    print_success "APK size: $APK_SIZE"
    
    # Check if APK is signed
    if command -v apksigner &> /dev/null; then
        if apksigner verify "$APK_PATH" &> /dev/null; then
            print_success "APK is properly signed"
        else
            print_error "APK is not signed or signature is invalid"
            print_warning "Make sure you have signing config in local.properties"
            exit 1
        fi
    fi
}

# Generate checksums
generate_checksums() {
    print_header "Generating Checksums"
    
    APK_PATH="$OUTPUT_DIR/$APK_NAME"
    
    # SHA256 (primary checksum for releases)
    if command -v sha256sum &> /dev/null; then
        SHA256=$(sha256sum "$APK_PATH" | cut -d' ' -f1)
        print_success "SHA256: $SHA256"
    elif command -v shasum &> /dev/null; then
        SHA256=$(shasum -a 256 "$APK_PATH" | cut -d' ' -f1)
        print_success "SHA256: $SHA256"
    else
        print_warning "No SHA256 tool found"
        SHA256="not-generated"
    fi
}

# Copy APK to release directory
copy_to_release() {
    print_header "Preparing Release Artifacts"
    
    # Create release directory
    mkdir -p "$RELEASE_DIR"
    
    # Use current version if NEW_VERSION_NAME is not set (build-only mode)
    if [ -z "$NEW_VERSION_NAME" ]; then
        get_current_version
        VERSION_FOR_RELEASE="$CURRENT_VERSION_NAME"
    else
        VERSION_FOR_RELEASE="$NEW_VERSION_NAME"
    fi
    
    # APK name for GitHub releases
    RELEASE_APK_NAME="${APP_NAME}-v${VERSION_FOR_RELEASE}.apk"
    RELEASE_APK_PATH="$RELEASE_DIR/$RELEASE_APK_NAME"
    
    # Copy APK
    cp "$OUTPUT_DIR/$APK_NAME" "$RELEASE_APK_PATH"
    print_success "APK copied: $RELEASE_APK_NAME"
    
    # Create SHA256 checksum file
    if [ -n "$SHA256" ] && [ "$SHA256" != "not-generated" ]; then
        echo "$SHA256  $RELEASE_APK_NAME" > "$RELEASE_APK_PATH.sha256"
        print_success "Checksum file created"
    fi
    
    print_info "Release artifacts in: $RELEASE_DIR/"
}

# Generate release notes
generate_release_notes() {
    print_header "Generating Release Notes"
    
    NOTES_FILE="$RELEASE_DIR/RELEASE_NOTES.md"
    BUILD_DATE=$(date +"%Y-%m-%d %H:%M:%S")
    GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    
    cat > "$NOTES_FILE" << EOF
# $APP_NAME v${NEW_VERSION_NAME}

## 📱 Release Information

- **Version:** ${NEW_VERSION_NAME}
- **Version Code:** ${NEW_VERSION_CODE}
- **Build Date:** ${BUILD_DATE}
- **Package:** ${PACKAGE_NAME}
- **Git Commit:** ${GIT_COMMIT}

## 📦 Download

\`\`\`
${APP_NAME}-v${NEW_VERSION_NAME}.apk
\`\`\`

**SHA256 Checksum:**
\`\`\`
${SHA256}
\`\`\`

## ✨ What's New

<!-- TODO: Add release notes here -->
- 

## 🐛 Bug Fixes

<!-- TODO: Add bug fixes here -->
- 

## 📋 Installation

### Method 1: Direct Install (Recommended)
1. Download \`${APP_NAME}-v${NEW_VERSION_NAME}.apk\`
2. Transfer to your Android TV device
3. Enable "Unknown sources" in Settings
4. Install using a file manager
5. Launch and enjoy!

### Method 2: ADB Install
\`\`\`bash
adb install ${APP_NAME}-v${NEW_VERSION_NAME}.apk
\`\`\`

## 🔄 Auto-Update

Users with previous versions can update directly from the app:
1. Open Settings → About
2. Click "Check for Updates"
3. Click "Download & Install"

## 📱 Requirements

- **Minimum:** Android 5.0 (API 21)
- **Target:** Android 14 (API 34)
- **Device:** Android TV or any Android device
- **Internet:** Required for API-based features

## 🔒 Security

- APK is signed and verified
- SHA256 checksum provided
- Direct download from GitHub releases

## 💬 Support

- **GitHub Issues:** https://github.com/${GITHUB_REPO}/issues
- **Developer:** mhdzumair

---

Built with ❤️ for the Muslim community
EOF
    
    print_success "Release notes: $NOTES_FILE"
    print_warning "Please edit $NOTES_FILE to add release details"
    echo ""
    
    # Offer to edit release notes
    read -p "Edit release notes now? [Y/n] " -r
    if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        # Detect default editor
        if [ -n "$EDITOR" ]; then
            $EDITOR "$NOTES_FILE"
        elif command -v nano &> /dev/null; then
            nano "$NOTES_FILE"
        elif command -v vim &> /dev/null; then
            vim "$NOTES_FILE"
        elif command -v vi &> /dev/null; then
            vi "$NOTES_FILE"
        else
            print_warning "No editor found, please edit manually: $NOTES_FILE"
        fi
    fi
}

# Commit version changes
commit_version() {
    print_header "Committing Version Changes"
    
    print_step "Staging $BUILD_GRADLE..."
    git add "$BUILD_GRADLE"
    
    print_step "Creating commit..."
    git commit -m "chore: bump version to v${NEW_VERSION_NAME}"
    
    print_success "Version changes committed"
    
    COMMIT_HASH=$(git rev-parse --short HEAD)
    print_info "Commit: $COMMIT_HASH"
}

# Create git tag
create_tag() {
    print_header "Creating Git Tag"
    
    TAG_NAME="v${NEW_VERSION_NAME}"
    
    # Check if tag already exists
    if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
        print_error "Tag $TAG_NAME already exists"
        exit 1
    fi
    
    print_step "Creating tag: $TAG_NAME..."
    git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
    
    print_success "Tag created: $TAG_NAME"
}

# Push changes to GitHub
push_to_github() {
    print_header "Pushing to GitHub"
    
    CURRENT_BRANCH=$(git branch --show-current)
    
    print_step "Pushing commits to $CURRENT_BRANCH..."
    git push origin "$CURRENT_BRANCH"
    print_success "Commits pushed"
    
    print_step "Pushing tag v${NEW_VERSION_NAME}..."
    git push origin "v${NEW_VERSION_NAME}"
    print_success "Tag pushed"
}

# Create GitHub release
create_github_release() {
    print_header "Creating GitHub Release"
    
    TAG_NAME="v${NEW_VERSION_NAME}"
    RELEASE_TITLE="$APP_NAME v${NEW_VERSION_NAME}"
    RELEASE_APK_NAME="${APP_NAME}-v${NEW_VERSION_NAME}.apk"
    RELEASE_APK_PATH="$RELEASE_DIR/$RELEASE_APK_NAME"
    NOTES_FILE="$RELEASE_DIR/RELEASE_NOTES.md"
    
    print_step "Creating release on GitHub..."
    print_info "Title: $RELEASE_TITLE"
    print_info "Tag: $TAG_NAME"
    echo ""
    
    # Create release with gh CLI
    gh release create "$TAG_NAME" \
        --title "$RELEASE_TITLE" \
        --notes-file "$NOTES_FILE" \
        "$RELEASE_APK_PATH" \
        "$RELEASE_APK_PATH.sha256"
    
    print_success "GitHub release created!"
    
    # Get release URL
    RELEASE_URL=$(gh release view "$TAG_NAME" --json url --jq .url)
    print_info "Release URL: $RELEASE_URL"
}

# Print summary
print_summary() {
    print_header "Release Summary"
    
    echo ""
    echo -e "${GREEN}${BOLD}🎉 GitHub Release v${NEW_VERSION_NAME} completed successfully!${NC}"
    echo ""
    echo -e "${BOLD}📊 Release Details:${NC}"
    echo "  ├─ Variant: github (self-update enabled)"
    echo "  ├─ Version: v${NEW_VERSION_NAME} (code: ${NEW_VERSION_CODE})"
    echo "  ├─ Tag: v${NEW_VERSION_NAME}"
    echo "  ├─ APK: ${APP_NAME}-v${NEW_VERSION_NAME}.apk"
    echo "  ├─ SHA256: ${SHA256:0:16}..."
    echo "  └─ Size: $(du -h "$RELEASE_DIR/${APP_NAME}-v${NEW_VERSION_NAME}.apk" | cut -f1)"
    echo ""
    echo -e "${BOLD}🔗 Links:${NC}"
    RELEASE_URL=$(gh release view "v${NEW_VERSION_NAME}" --json url --jq .url 2>/dev/null || echo "https://github.com/${GITHUB_REPO}/releases/tag/v${NEW_VERSION_NAME}")
    echo "  ├─ Release: $RELEASE_URL"
    echo "  ├─ Download: $RELEASE_URL"
    echo "  └─ Repository: https://github.com/${GITHUB_REPO}"
    echo ""
    echo -e "${BOLD}📱 Users can now:${NC}"
    echo "  1. Download APK from GitHub releases"
    echo "  2. Update via in-app updater (Settings → About → Check for Updates)"
    echo ""
    echo -e "${BOLD}📦 For Play Store release:${NC}"
    echo "  ./gradlew bundlePlaystoreRelease"
    echo ""
    echo -e "${GREEN}✓ All done! Your release is live on GitHub${NC}"
    echo ""
}

# Print build-only summary
print_build_summary() {
    print_header "Build Summary"
    
    get_current_version
    
    echo ""
    echo -e "${GREEN}${BOLD}✓ GitHub variant build completed successfully!${NC}"
    echo ""
    echo -e "${BOLD}📦 Build Details:${NC}"
    echo "  ├─ Variant: github (self-update enabled)"
    echo "  ├─ Version: v${CURRENT_VERSION_NAME} (code: ${CURRENT_VERSION_CODE})"
    echo "  ├─ APK: ${APP_NAME}-v${CURRENT_VERSION_NAME}.apk"
    echo "  ├─ Location: $RELEASE_DIR/${APP_NAME}-v${CURRENT_VERSION_NAME}.apk"
    echo "  ├─ SHA256: ${SHA256:0:16}..."
    echo "  └─ Size: $(du -h "$RELEASE_DIR/${APP_NAME}-v${CURRENT_VERSION_NAME}.apk" | cut -f1)"
    echo ""
    echo -e "${BOLD}📱 Test Installation:${NC}"
    echo "  adb install $RELEASE_DIR/${APP_NAME}-v${CURRENT_VERSION_NAME}.apk"
    echo ""
    echo -e "${BOLD}📦 For Play Store:${NC}"
    echo "  ./gradlew bundlePlaystoreRelease"
    echo ""
    echo -e "${GREEN}✓ APK ready for testing!${NC}"
    echo ""
}

# Main execution
main() {
    clear
    
    echo -e "${CYAN}${BOLD}"
    if [ "$BUILD_ONLY" = true ]; then
        echo "╔══════════════════════════════════════════════════════╗"
        echo "║                                                      ║"
        echo "║     MosqueClock - Build GitHub APK Only             ║"
        echo "║         (with self-update feature)                  ║"
        echo "║                                                      ║"
        echo "╚══════════════════════════════════════════════════════╝"
    else
        echo "╔══════════════════════════════════════════════════════╗"
        echo "║                                                      ║"
        echo "║     MosqueClock - Automated GitHub Release          ║"
        echo "║         (with self-update feature)                  ║"
        echo "║                                                      ║"
        echo "╚══════════════════════════════════════════════════════╝"
    fi
    echo -e "${NC}"
    
    check_prerequisites
    
    # Build-only mode: skip version prompts and git operations
    if [ "$BUILD_ONLY" = true ]; then
        print_info "Build-only mode: Building current version without version bump or release"
        if [ "$NO_CLEAN" = true ]; then
            print_info "Incremental build mode: Skipping clean step for faster builds"
        fi
        echo ""
        read -p "Continue with build? [Y/n] " -r
        if [[ $REPLY =~ ^[Nn]$ ]]; then
            print_info "Build cancelled"
            exit 0
        fi
        
        clean_build
        build_release
        verify_apk
        generate_checksums
        copy_to_release
        print_build_summary
        return
    fi
    
    # Full release mode
    prompt_new_version
    
    echo ""
    print_warning "This will:"
    echo "  1. Update version in build.gradle.kts"
    if [ "$NO_CLEAN" = true ]; then
        echo "  2. Build GitHub release APK (incremental, no clean)"
    else
        echo "  2. Clean and build GitHub release APK"
    fi
    echo "  3. Generate checksums"
    echo "  4. Commit version changes"
    echo "  5. Create git tag v${NEW_VERSION_NAME}"
    echo "  6. Push to GitHub"
    echo "  7. Create GitHub release with APK"
    echo ""
    read -p "Continue with automated release? [Y/n] " -r
    if [[ $REPLY =~ ^[Nn]$ ]]; then
        print_info "Release cancelled"
        exit 0
    fi
    
    update_version
    clean_build
    build_release
    verify_apk
    generate_checksums
    copy_to_release
    generate_release_notes
    commit_version
    create_tag
    push_to_github
    create_github_release
    print_summary
}

# Run main function
main "$@"

