#!/bin/bash
# Configure Swift AppNote for standalone mode with SPM
# This script helps set up the appnote to use MobileIDSDK from SPM instead of local build

set -e

echo "Configuring Swift AppNote for standalone mode..."

# Check if we're in the right directory
if [ ! -f "appnote-swift.xcodeproj/project.pbxproj" ]; then
    echo "Error: Must be run from appnote/swift directory"
    exit 1
fi

# Read VERSION from parent directory
if [ -f "../../VERSION" ]; then
    VERSION=$(cat ../../VERSION | tr -d '\n\r')
    echo "Using SDK version: $VERSION"
else
    echo "Error: VERSION file not found at ../../VERSION"
    exit 1
fi

echo ""
echo "================================================================"
echo "To configure this Swift appnote for standalone mode:"
echo "================================================================"
echo ""
echo "1. Open appnote-swift.xcodeproj in Xcode"
echo ""
echo "2. Remove the local MobileIDSdk.xcframework reference:"
echo "   - In Project Navigator, select 'MobileIDSdk.xcframework'"
echo "   - Press Delete and choose 'Remove Reference'"
echo ""
echo "3. Add the SPM package:"
echo "   - Go to File > Add Package Dependencies..."
echo "   - Enter: https://github.com/baltech-ag/MobileIDSDK-pkg"
echo "   - Select version: $VERSION (or appropriate version)"
echo "   - Add 'MobileIDSdk' to your target"
echo ""
echo "4. Update Framework Search Paths:"
echo "   - Select the project in Project Navigator"
echo "   - Select the 'appnote-swift' target"
echo "   - Go to Build Settings > Search Paths > Framework Search Paths"
echo "   - Remove the path: \$(PROJECT_DIR)/../../sdk/build/XCFrameworks/debug"
echo ""
echo "5. Build and run!"
echo ""
echo "================================================================"
echo "To switch back to local/development mode:"
echo "================================================================"
echo ""
echo "Run this from the main repository root:"
echo "  cd appnote/swift"
echo "  ./configure-for-development.sh"
echo ""
