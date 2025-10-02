#!/bin/bash

# Mosque Prayer Clock - GitHub Release Build Script
# Creates production-ready release APK for GitHub releases
# Compatible with auto-updater functionality

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="MosqueClock"
PACKAGE_NAME="com.mosque.prayerclock"
OUTPUT_DIR="app/build/outputs/apk/release"
APK_NAME="app-release.apk"
APK_RELEASE_NAME="MosqueClock"
RELEASE_DIR="release"

# Functions
print_header() {
    echo -e "${GREEN}================================================${NC}"
    echo -e "${GREEN}  $1${NC}"
    echo -e "${GREEN}================================================${NC}"
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

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check if gradlew exists
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew not found. Are you in the project root?"
        exit 1
    fi
    print_success "Gradle wrapper found"
    
    # Make gradlew executable
    chmod +x ./gradlew
    print_success "Made gradlew executable"
    
    # Check git
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed"
        exit 1
    fi
    print_success "Git found"
}

# Get version information
get_version_info() {
    print_header "Version Information"
    
    # Get version from build.gradle.kts
    VERSION_NAME=$(grep 'versionName = ' app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
    VERSION_CODE=$(grep 'versionCode = ' app/build.gradle.kts | sed 's/.*= \(.*\)/\1/' | tr -d ' ')
    
    if [ -z "$VERSION_NAME" ]; then
        print_error "Could not determine version from build.gradle.kts"
        exit 1
    fi
    
    print_info "Version Name: $VERSION_NAME"
    print_info "Version Code: $VERSION_CODE"
    
    # Check if this version has a git tag
    if git rev-parse "v$VERSION_NAME" >/dev/null 2>&1; then
        print_info "Git tag v$VERSION_NAME exists"
        GIT_TAG="v$VERSION_NAME"
    else
        print_info "No git tag found for v$VERSION_NAME"
        GIT_TAG=""
    fi
    
    # Get git commit
    GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    print_info "Git Commit: $GIT_COMMIT"
}

# Clean previous builds
clean_build() {
    print_header "Cleaning Previous Builds"
    print_step "Removing old build artifacts..."
    
    ./gradlew clean
    
    print_success "Clean completed"
}

# Build release APK
build_release() {
    print_header "Building Release APK"
    print_step "Building with ProGuard optimization..."
    print_info "This may take a few minutes..."
    
    ./gradlew assembleRelease
    
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
            print_info "For GitHub releases, make sure you have signing config in local.properties"
        fi
    fi
    
    # Get APK info using aapt (if available)
    if command -v aapt &> /dev/null; then
        print_step "APK Information:"
        aapt dump badging "$APK_PATH" | grep -E "package|sdkVersion|targetSdkVersion|application-label"
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
        print_error "No SHA256 tool found"
        SHA256="not-generated"
    fi
}

# Copy APK to release directory
copy_to_release() {
    print_header "Preparing GitHub Release Artifacts"
    
    # Create timestamped release directory
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    VERSION_DIR="$RELEASE_DIR/v${VERSION_NAME}_$TIMESTAMP"
    mkdir -p "$VERSION_DIR"
    
    # Universal APK name for GitHub releases
    # Format: MosqueClock-v1.0.apk
    RELEASE_APK_NAME="${APK_RELEASE_NAME}-v${VERSION_NAME}.apk"
    
    # Copy APK
    cp "$OUTPUT_DIR/$APK_NAME" "$VERSION_DIR/$RELEASE_APK_NAME"
    print_success "APK copied: $RELEASE_APK_NAME"
    
    # Create SHA256 checksum file
    if [ -n "$SHA256" ] && [ "$SHA256" != "not-generated" ]; then
        echo "$SHA256  $RELEASE_APK_NAME" > "$VERSION_DIR/$RELEASE_APK_NAME.sha256"
        print_success "Checksum file created"
    fi
    
    print_info "Release artifacts in: $VERSION_DIR/"
    
    # Also copy to root release directory for easy access
    cp "$VERSION_DIR/$RELEASE_APK_NAME" "$RELEASE_DIR/$RELEASE_APK_NAME"
    print_success "Latest release APK: $RELEASE_DIR/$RELEASE_APK_NAME"
}

# Generate release notes for GitHub
generate_release_notes() {
    print_header "Generating GitHub Release Notes"
    
    BUILD_DATE=$(date +"%Y-%m-%d %H:%M:%S")
    
    VERSION_DIR="$RELEASE_DIR/v${VERSION_NAME}_$(date +"%Y%m%d_%H%M%S")"
    NOTES_FILE="$VERSION_DIR/RELEASE_NOTES.md"
    
    cat > "$NOTES_FILE" << EOF
# MosqueClock v${VERSION_NAME}

## 📱 Release Information

- **Version:** ${VERSION_NAME}
- **Build Date:** ${BUILD_DATE}
- **Package:** ${PACKAGE_NAME}
- **Git Commit:** ${GIT_COMMIT}

## 📦 Download

\`\`\`
${APK_RELEASE_NAME}-v${VERSION_NAME}.apk
\`\`\`

**SHA256 Checksum:**
\`\`\`
${SHA256}
\`\`\`

## ✨ Features

- 🕌 **Prayer Times Display** - Multiple sources (ACJU Direct, MosqueClock API, Al-Adhan API, Manual)
- 🌍 **Multi-Language Support** - English, Tamil, Sinhala
- 🌤️ **Weather Integration** - WeatherAPI.com & OpenWeatherMap support
- 🔔 **Prayer Countdown** - Audio and full-screen countdown alerts
- 📅 **Hijri Calendar** - Multiple data sources
- 🎨 **Customizable Themes** - Multiple color schemes
- ⚙️ **In-App Configuration** - No build-time setup required
- 🔄 **Auto-Update** - Check for updates from GitHub releases

## 🔧 Configuration

All configuration is done in-app:

1. Open **Settings** (⚙️)
2. Configure **Prayer Service** and select your zone/region
3. Enable **Weather** and add your API key (optional)
4. Set **Mosque Name** and preferences

### API Keys (Optional)

- **WeatherAPI.com**: Get free key from https://www.weatherapi.com/
- **OpenWeatherMap**: Get free key from https://openweathermap.org/api
- **MosqueClock Backend**: Configure your backend URL and API key (if using)

## 📋 Installation

### Method 1: Direct Install (Recommended)
1. Download \`${APK_RELEASE_NAME}-v${VERSION_NAME}.apk\`
2. Transfer to your Android TV device
3. Enable "Unknown sources" in Settings
4. Install using a file manager
5. Launch and configure

### Method 2: ADB Install
\`\`\`bash
adb install ${APK_RELEASE_NAME}-v${VERSION_NAME}.apk
\`\`\`

## 🚀 Post-Installation Setup (Optional)

For optimal operation on Android TV, run these ADB commands:

\`\`\`bash
# Allow display over other apps
adb shell appops set ${PACKAGE_NAME} SYSTEM_ALERT_WINDOW allow

# Disable battery optimization
adb shell dumpsys deviceidle whitelist +${PACKAGE_NAME}

# Keep screen on (optional)
adb shell settings put system screen_off_timeout 2147483647
\`\`\`

## 📱 Requirements

- **Minimum:** Android 5.0 (API 21)
- **Target:** Android 14 (API 34)
- **Device:** Android TV or any Android device
- **Internet:** Required for API-based prayer times and weather

## 🔒 Security

- APK is signed and verified
- No build-time secrets included
- All API keys configured in-app
- Secure data storage using DataStore

## 📝 Technical Details

- **Build Type:** Release (ProGuard optimized)
- **Code Minification:** Enabled
- **Resource Shrinking:** Enabled
- **Debug Logging:** Disabled
- **APK Size:** ~10MB

## 🐛 Known Issues

None reported for this version.

## 💬 Support

- **GitHub Issues:** https://github.com/mhdzumair/MosqueClock/issues
- **Developer:** mhdzumair

## 📜 License

Open Source - See LICENSE file in repository

---

Built with ❤️ for the Muslim community
EOF
    
    print_success "Release notes: $NOTES_FILE"
}

# Create GitHub release checklist
create_release_checklist() {
    print_header "GitHub Release Checklist"
    
    VERSION_DIR="$RELEASE_DIR/v${VERSION_NAME}_$(date +"%Y%m%d_%H%M%S")"
    CHECKLIST_FILE="$VERSION_DIR/GITHUB_RELEASE_CHECKLIST.txt"
    
    cat > "$CHECKLIST_FILE" << EOF
===============================================
GitHub Release Checklist - v${VERSION_NAME}
===============================================

PRE-RELEASE
-----------
☐ Test APK on Android TV device
☐ Verify all features work correctly
☐ Test auto-updater checks this release
☐ Verify APK signature is valid
☐ Check SHA256 checksum matches

CREATE GITHUB RELEASE
--------------------
☐ Go to: https://github.com/mhdzumair/MosqueClock/releases/new
☐ Create new tag: v${VERSION_NAME}
☐ Release title: "${APP_NAME} v${VERSION_NAME}"
☐ Copy content from RELEASE_NOTES.md
☐ Upload: ${APK_RELEASE_NAME}-v${VERSION_NAME}.apk
☐ Upload: ${APK_RELEASE_NAME}-v${VERSION_NAME}.apk.sha256
☐ Set as latest release: ✓
☐ Publish release

POST-RELEASE
------------
☐ Test auto-updater from app
☐ Verify download link works
☐ Update README.md if needed
☐ Announce release (if applicable)

NOTES
-----
- Auto-updater will check: https://api.github.com/repos/mhdzumair/MosqueClock/releases/latest
- APK must be attached to release for download
- Tag format must be: v${VERSION_NAME} (starts with 'v')

===============================================
EOF
    
    print_success "Checklist created: $CHECKLIST_FILE"
}

# Print summary
print_summary() {
    print_header "Build Summary"
    
    echo ""
    echo -e "${GREEN}✓ Release build completed successfully!${NC}"
    echo ""
    echo -e "${BLUE}📦 Release Artifacts:${NC}"
    echo "  └─ $RELEASE_DIR/"
    echo "     ├─ ${APK_RELEASE_NAME}-v${VERSION_NAME}.apk  (Upload to GitHub)"
    echo "     └─ v${VERSION_NAME}_*/"
    echo "        ├─ ${APK_RELEASE_NAME}-v${VERSION_NAME}.apk"
    echo "        ├─ ${APK_RELEASE_NAME}-v${VERSION_NAME}.apk.sha256"
    echo "        ├─ RELEASE_NOTES.md"
    echo "        └─ GITHUB_RELEASE_CHECKLIST.txt"
    echo ""
    echo -e "${BLUE}📋 Next Steps:${NC}"
    echo "  1. Test APK: $RELEASE_DIR/${APK_RELEASE_NAME}-v${VERSION_NAME}.apk"
    echo "  2. Create git tag: git tag v${VERSION_NAME} && git push origin v${VERSION_NAME}"
    echo "  3. Create GitHub release: https://github.com/mhdzumair/MosqueClock/releases/new"
    echo "  4. Upload APK and checksums"
    echo "  5. Copy release notes from RELEASE_NOTES.md"
    echo ""
    echo -e "${YELLOW}⚠️  Important for Auto-Updater:${NC}"
    echo "  - Tag must be: v${VERSION_NAME}"
    echo "  - APK name must be: ${APK_RELEASE_NAME}-v${VERSION_NAME}.apk"
    echo "  - Mark as 'Latest release'"
    echo ""
}

# Main execution
main() {
    clear
    print_header "$APP_NAME - GitHub Release Build"
    
    check_prerequisites
    get_version_info
    
    echo ""
    read -p "Build release APK for v${VERSION_NAME}? (y/n) " -n 1 -r
    echo ""
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Build cancelled"
        exit 0
    fi
    
    clean_build
    build_release
    verify_apk
    generate_checksums
    copy_to_release
    generate_release_notes
    create_release_checklist
    print_summary
    
    echo -e "${GREEN}🎉 Ready for GitHub release!${NC}"
    echo ""
}

# Run main function
main "$@"
