#!/bin/bash

# Mosque Prayer Clock - Release Build Script
# This script automates the release build process for Android TV

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="Mosque Prayer Clock"
PACKAGE_NAME="com.mosque.prayerclock"
OUTPUT_DIR="app/build/outputs/apk/release"
APK_NAME="app-release.apk"

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

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check if gradlew exists
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew not found. Are you in the project root?"
        exit 1
    fi
    print_success "Gradle wrapper found"
    
    # Check if local.properties exists
    if [ ! -f "local.properties" ]; then
        print_error "local.properties not found"
        echo "Please create local.properties with required API keys"
        echo "See local.properties.example for reference"
        exit 1
    fi
    print_success "local.properties found"
    
    # Make gradlew executable
    chmod +x ./gradlew
    print_success "Made gradlew executable"
}

# Clean previous builds
clean_build() {
    print_header "Cleaning Previous Builds"
    print_step "Removing old build artifacts..."
    
    ./gradlew clean
    
    if [ -d "$OUTPUT_DIR" ]; then
        rm -rf "$OUTPUT_DIR"
        print_success "Cleaned output directory"
    fi
    
    print_success "Clean completed"
}

# Run lint checks
run_lint() {
    print_header "Running Lint Checks"
    print_step "Analyzing code..."
    
    ./gradlew lintRelease || {
        print_error "Lint check failed"
        echo "Check reports at: app/build/reports/lint-results-release.html"
        read -p "Continue anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    }
    
    print_success "Lint checks passed"
}

# Build release APK
build_release() {
    print_header "Building Release APK"
    print_step "Compiling and packaging..."
    
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
    
    # MD5
    if command -v md5sum &> /dev/null; then
        MD5=$(md5sum "$APK_PATH" | cut -d' ' -f1)
        echo "$MD5" > "$OUTPUT_DIR/app-release.md5"
        print_success "MD5: $MD5"
    elif command -v md5 &> /dev/null; then
        MD5=$(md5 -q "$APK_PATH")
        echo "$MD5" > "$OUTPUT_DIR/app-release.md5"
        print_success "MD5: $MD5"
    fi
    
    # SHA256
    if command -v sha256sum &> /dev/null; then
        SHA256=$(sha256sum "$APK_PATH" | cut -d' ' -f1)
        echo "$SHA256" > "$OUTPUT_DIR/app-release.sha256"
        print_success "SHA256: $SHA256"
    elif command -v shasum &> /dev/null; then
        SHA256=$(shasum -a 256 "$APK_PATH" | cut -d' ' -f1)
        echo "$SHA256" > "$OUTPUT_DIR/app-release.sha256"
        print_success "SHA256: $SHA256"
    fi
}

# Optional: Install on connected device
install_on_device() {
    print_header "Installation"
    
    # Check if adb is available
    if ! command -v adb &> /dev/null; then
        print_error "ADB not found. Skipping installation."
        return
    fi
    
    # Check for connected devices
    DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
    
    if [ "$DEVICES" -eq 0 ]; then
        print_step "No devices connected. Skipping installation."
        return
    fi
    
    read -p "Install APK on connected device? (y/n) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_step "Installing APK..."
        adb install -r "$OUTPUT_DIR/$APK_NAME"
        print_success "APK installed successfully"
        
        # Optional: Launch app
        read -p "Launch app on device? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            adb shell am start -n "$PACKAGE_NAME/.SplashActivity"
            print_success "App launched"
        fi
    fi
}

# Copy APK to release directory
copy_to_release() {
    print_header "Copying Release Artifacts"
    
    RELEASE_DIR="release"
    mkdir -p "$RELEASE_DIR"
    
    # Get version from build.gradle.kts
    VERSION=$(grep "versionName" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    
    APK_RELEASE_NAME="MosquePrayerClock-v${VERSION}-${TIMESTAMP}.apk"
    
    cp "$OUTPUT_DIR/$APK_NAME" "$RELEASE_DIR/$APK_RELEASE_NAME"
    
    if [ -f "$OUTPUT_DIR/app-release.md5" ]; then
        cp "$OUTPUT_DIR/app-release.md5" "$RELEASE_DIR/$APK_RELEASE_NAME.md5"
    fi
    
    if [ -f "$OUTPUT_DIR/app-release.sha256" ]; then
        cp "$OUTPUT_DIR/app-release.sha256" "$RELEASE_DIR/$APK_RELEASE_NAME.sha256"
    fi
    
    print_success "Release artifacts copied to: $RELEASE_DIR/"
    print_success "APK: $APK_RELEASE_NAME"
}

# Generate release notes
generate_release_notes() {
    print_header "Generating Release Notes"
    
    VERSION=$(grep "versionName" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
    BUILD_DATE=$(date +"%Y-%m-%d %H:%M:%S")
    
    NOTES_FILE="release/RELEASE_NOTES.txt"
    
    cat > "$NOTES_FILE" << EOF
===============================================
$APP_NAME - Release Build
===============================================

Version: $VERSION
Build Date: $BUILD_DATE
Package: $PACKAGE_NAME

BUILD INFORMATION
-----------------
- Optimized for Android TV
- Code minification: Enabled (ProGuard)
- Resource shrinking: Enabled
- Debug logging: Disabled
- Target SDK: 34 (Android 14)
- Minimum SDK: 21 (Android 5.0)

FEATURES
--------
- Prayer times display with countdown
- Multiple prayer time sources (ACJU, PrayerTimes.co.uk)
- Weather integration
- Multi-language support (English, Tamil, Sinhala)
- Customizable themes
- Auto-start on boot
- Always-on display
- Remote control navigation optimized

INSTALLATION
------------
1. Copy APK to Android TV device
2. Enable "Unknown sources" in Settings
3. Install APK using a file manager
4. Launch and configure

POST-INSTALLATION SETUP
-----------------------
Run these ADB commands for optimal operation:

# Allow display over other apps
adb shell appops set $PACKAGE_NAME SYSTEM_ALERT_WINDOW allow

# Disable battery optimization
adb shell dumpsys deviceidle whitelist +$PACKAGE_NAME

# Keep screen on
adb shell settings put system screen_off_timeout 2147483647

CHECKSUMS
---------
EOF
    
    if [ -f "$OUTPUT_DIR/app-release.md5" ]; then
        echo "MD5:    $(cat $OUTPUT_DIR/app-release.md5)" >> "$NOTES_FILE"
    fi
    
    if [ -f "$OUTPUT_DIR/app-release.sha256" ]; then
        echo "SHA256: $(cat $OUTPUT_DIR/app-release.sha256)" >> "$NOTES_FILE"
    fi
    
    cat >> "$NOTES_FILE" << EOF

SUPPORT
-------
For issues or questions, please contact support.

===============================================
EOF
    
    print_success "Release notes generated: $NOTES_FILE"
}

# Print summary
print_summary() {
    print_header "Build Summary"
    
    echo ""
    echo "✓ Build completed successfully!"
    echo ""
    echo "Artifacts location:"
    echo "  - APK: $OUTPUT_DIR/$APK_NAME"
    echo "  - Release copy: release/"
    echo "  - Lint report: app/build/reports/lint-results-release.html"
    echo "  - ProGuard mapping: app/build/outputs/mapping/release/mapping.txt"
    echo ""
    echo "Next steps:"
    echo "  1. Test the APK on an Android TV device"
    echo "  2. Verify all features work correctly"
    echo "  3. Check release notes in release/RELEASE_NOTES.txt"
    echo "  4. Deploy to target devices"
    echo ""
}

# Main execution
main() {
    print_header "$APP_NAME - Release Build Script"
    
    check_prerequisites
    clean_build
    run_lint
    build_release
    verify_apk
    generate_checksums
    copy_to_release
    generate_release_notes
    install_on_device
    print_summary
}

# Run main function
main "$@"
