#!/bin/bash

# Mosque Prayer Clock - Build All APK Variants
# This script builds APKs for all CPU architectures plus a universal APK

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
    
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew not found. Are you in the project root?"
        exit 1
    fi
    print_success "Gradle wrapper found"
    
    if [ ! -f "local.properties" ]; then
        print_error "local.properties not found"
        echo "Please create local.properties with required API keys"
        exit 1
    fi
    print_success "local.properties found"
    
    chmod +x ./gradlew
    print_success "Made gradlew executable"
}

# Clean previous builds
clean_build() {
    print_header "Cleaning Previous Builds"
    print_step "Removing old build artifacts..."
    
    ./gradlew clean
    
    print_success "Clean completed"
}

# Build all release APK variants
build_apks() {
    print_header "Building All APK Variants"
    print_step "This will create APKs for:"
    print_info "  - ARM v7 (armeabi-v7a) - 32-bit ARM devices"
    print_info "  - ARM v8 (arm64-v8a) - 64-bit ARM devices (most Android TVs)"
    print_info "  - x86 - 32-bit Intel/AMD devices"
    print_info "  - x86_64 - 64-bit Intel/AMD devices (emulators)"
    print_info "  - Universal APK - Works on all devices (larger size)"
    echo ""
    
    print_step "Building release APKs..."
    ./gradlew assembleRelease
    
    print_success "All APK variants built successfully"
}

# List generated APKs
list_apks() {
    print_header "Generated APK Files"
    
    OUTPUT_DIR="app/build/outputs/apk/release"
    
    if [ -d "$OUTPUT_DIR" ]; then
        echo ""
        print_step "APK files in $OUTPUT_DIR:"
        echo ""
        
        # Find and display all APKs with their sizes
        find "$OUTPUT_DIR" -name "*.apk" -type f | while read apk; do
            filename=$(basename "$apk")
            size=$(du -h "$apk" | cut -f1)
            
            if [[ $filename == *"universal"* ]]; then
                echo -e "  ${GREEN}📦 $filename${NC} (${size}) - Universal (all devices)"
            elif [[ $filename == *"arm64-v8a"* ]]; then
                echo -e "  ${BLUE}📦 $filename${NC} (${size}) - ARM 64-bit (most Android TVs)"
            elif [[ $filename == *"armeabi-v7a"* ]]; then
                echo -e "  ${BLUE}📦 $filename${NC} (${size}) - ARM 32-bit"
            elif [[ $filename == *"x86_64"* ]]; then
                echo -e "  ${YELLOW}📦 $filename${NC} (${size}) - Intel/AMD 64-bit (emulators)"
            elif [[ $filename == *"x86"* ]]; then
                echo -e "  ${YELLOW}📦 $filename${NC} (${size}) - Intel/AMD 32-bit"
            else
                echo -e "  📦 $filename (${size})"
            fi
        done
        echo ""
    else
        print_error "Output directory not found!"
        exit 1
    fi
}

# Generate checksums
generate_checksums() {
    print_header "Generating Checksums"
    
    OUTPUT_DIR="app/build/outputs/apk/release"
    
    find "$OUTPUT_DIR" -name "*.apk" -type f | while read apk; do
        filename=$(basename "$apk")
        print_step "Generating checksums for $filename..."
        
        # MD5
        if command -v md5sum &> /dev/null; then
            md5sum "$apk" | cut -d' ' -f1 > "$apk.md5"
        elif command -v md5 &> /dev/null; then
            md5 -q "$apk" > "$apk.md5"
        fi
        
        # SHA256
        if command -v sha256sum &> /dev/null; then
            sha256sum "$apk" | cut -d' ' -f1 > "$apk.sha256"
        elif command -v shasum &> /dev/null; then
            shasum -a 256 "$apk" | cut -d' ' -f1 > "$apk.sha256"
        fi
    done
    
    print_success "Checksums generated"
}

# Create organized release directory
organize_releases() {
    print_header "Organizing Release Files"
    
    RELEASE_DIR="release-apks"
    VERSION=$(grep "versionName" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/' | head -1)
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    RELEASE_SUBDIR="$RELEASE_DIR/v${VERSION}_${TIMESTAMP}"
    
    mkdir -p "$RELEASE_SUBDIR"
    
    print_step "Copying APKs to $RELEASE_SUBDIR..."
    
    OUTPUT_DIR="app/build/outputs/apk/release"
    find "$OUTPUT_DIR" -name "*.apk" -type f -exec cp {} "$RELEASE_SUBDIR/" \;
    find "$OUTPUT_DIR" -name "*.md5" -type f -exec cp {} "$RELEASE_SUBDIR/" \;
    find "$OUTPUT_DIR" -name "*.sha256" -type f -exec cp {} "$RELEASE_SUBDIR/" \;
    
    print_success "Release files organized in $RELEASE_SUBDIR"
}

# Generate release notes
generate_release_notes() {
    print_header "Generating Release Notes"
    
    RELEASE_DIR="release-apks"
    VERSION=$(grep "versionName" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/' | head -1)
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    RELEASE_SUBDIR="$RELEASE_DIR/v${VERSION}_${TIMESTAMP}"
    NOTES_FILE="$RELEASE_SUBDIR/RELEASE_NOTES.txt"
    
    cat > "$NOTES_FILE" << EOF
===============================================
Mosque Prayer Clock for Sri Lanka
Android TV Application - Release Build
===============================================

Version: $VERSION
Build Date: $(date +"%Y-%m-%d %H:%M:%S")
Build Type: Release (Signed & Optimized)

APK VARIANTS
------------
This release includes multiple APK variants optimized for different devices:

1. app-universal-release.apk
   - Works on ALL devices (recommended for general distribution)
   - Larger file size but maximum compatibility
   - Install this if unsure which variant to use

2. app-arm64-v8a-release.apk
   - Optimized for 64-bit ARM devices
   - Best for most modern Android TV boxes
   - Smallest file size for ARM64 devices
   - Recommended for: Mi Box, Fire TV, Shield TV, most Android TVs

3. app-armeabi-v7a-release.apk
   - Optimized for 32-bit ARM devices
   - For older Android TV boxes
   - Smaller file size for ARM32 devices

4. app-x86_64-release.apk
   - Optimized for 64-bit Intel/AMD devices
   - Best for emulators and x86-based Android devices
   - Ideal for testing on Android Studio Emulator

5. app-x86-release.apk
   - Optimized for 32-bit Intel/AMD devices
   - For older x86-based Android devices

WHICH APK SHOULD I INSTALL?
---------------------------
• If you're unsure: Use app-universal-release.apk
• For most Android TV devices: Use app-arm64-v8a-release.apk
• For Android emulator: Use app-x86_64-release.apk
• For older devices: Try armeabi-v7a or x86 variants

INSTALLATION
------------
1. Transfer the appropriate APK to your Android TV device
2. Enable "Unknown sources" in Settings > Security
3. Install the APK using a file manager
4. Run the setup commands to grant permissions

POST-INSTALLATION SETUP
----------------------
After installation, run these ADB commands for optimal operation:

# Grant essential permissions
adb shell appops set com.mosque.prayerclock SYSTEM_ALERT_WINDOW allow
adb shell dumpsys deviceidle whitelist +com.mosque.prayerclock

# Configure display
adb shell settings put system screen_off_timeout 2147483647
adb shell settings put system screen_brightness 255

FEATURES
--------
✓ Direct ACJU web scraping with PDF parsing
✓ Multiple prayer time sources
✓ Weather integration (WeatherAPI.com, OpenWeatherMap)
✓ Multi-language support (English, Tamil, Sinhala)
✓ Digital and Analog clock displays
✓ Hijri date display
✓ Auto-start on boot
✓ Always-on display support
✓ 13 Sri Lankan prayer time zones

TECHNICAL DETAILS
-----------------
- Min SDK: 21 (Android 5.0)
- Target SDK: 34 (Android 14)
- ProGuard: Enabled (optimized & obfuscated)
- Signed: Yes (release keystore)

APK SIZES
---------
EOF
    
    # Add APK sizes to release notes
    OUTPUT_DIR="app/build/outputs/apk/release"
    find "$OUTPUT_DIR" -name "*.apk" -type f | while read apk; do
        filename=$(basename "$apk")
        size=$(du -h "$apk" | cut -f1)
        echo "  $filename: $size" >> "$NOTES_FILE"
    done
    
    cat >> "$NOTES_FILE" << EOF

CHECKSUMS
---------
See individual .md5 and .sha256 files for verification.

SUPPORT
-------
For issues or questions, visit the project repository.

===============================================
EOF
    
    print_success "Release notes generated: $NOTES_FILE"
}

# Print summary
print_summary() {
    print_header "Build Summary"
    
    RELEASE_DIR="release-apks"
    VERSION=$(grep "versionName" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/' | head -1)
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    RELEASE_SUBDIR="$RELEASE_DIR/v${VERSION}_${TIMESTAMP}"
    
    echo ""
    echo "✓ All APK variants built successfully!"
    echo ""
    echo "📦 Release files location:"
    echo "   $RELEASE_SUBDIR/"
    echo ""
    echo "📋 APK Variants:"
    echo "   • Universal APK (works on all devices)"
    echo "   • ARM 64-bit APK (most Android TVs)"
    echo "   • ARM 32-bit APK (older devices)"
    echo "   • x86 64-bit APK (emulators)"
    echo "   • x86 32-bit APK (x86 devices)"
    echo ""
    echo "📄 Additional files:"
    echo "   • MD5 and SHA256 checksums for each APK"
    echo "   • RELEASE_NOTES.txt with installation guide"
    echo ""
    echo "🚀 Next steps:"
    echo "   1. Test the appropriate APK on your target device"
    echo "   2. Distribute APKs to users"
    echo "   3. Provide RELEASE_NOTES.txt for installation instructions"
    echo ""
}

# Main execution
main() {
    print_header "Mosque Prayer Clock - Build All APK Variants"
    
    check_prerequisites
    clean_build
    build_apks
    list_apks
    generate_checksums
    organize_releases
    generate_release_notes
    print_summary
}

# Run main function
main "$@"

