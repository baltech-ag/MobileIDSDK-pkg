#!/bin/bash
# Configure Swift AppNote for development mode with local XCFramework
# This script helps set up the appnote to use local SDK build

set -e

echo "Configuring Swift AppNote for development mode..."

# Check if we're in the right directory
if [ ! -f "appnote-swift.xcodeproj/project.pbxproj" ]; then
    echo "Error: Must be run from appnote/swift directory"
    exit 1
fi

echo ""
echo "================================================================"
echo "To configure this Swift appnote for development mode:"
echo "================================================================"
echo ""
echo "1. Build the SDK XCFramework first:"
echo "   cd ../../"
echo "   ./gradlew :sdk:assembleMobileIDSdkXCFramework"
echo ""
echo "2. Open appnote-swift.xcodeproj in Xcode"
echo ""
echo "3. Remove the SPM package if present:"
echo "   - In Project Navigator, find 'MobileIDSdk' under Package Dependencies"
echo "   - Right-click and choose 'Remove Package'"
echo ""
echo "4. Add the local XCFramework:"
echo "   - Right-click on the project and choose 'Add Files to appnote-swift...'"
echo "   - Navigate to: ../../sdk/build/XCFrameworks/debug/"
echo "   - Select MobileIDSdk.xcframework"
echo "   - Ensure 'Copy items if needed' is UNCHECKED"
echo "   - Click Add"
echo ""
echo "5. Update Framework Search Paths (if needed):"
echo "   - Select the project in Project Navigator"
echo "   - Select the 'appnote-swift' target"
echo "   - Go to Build Settings > Search Paths > Framework Search Paths"
echo "   - Add: \$(PROJECT_DIR)/../../sdk/build/XCFrameworks/debug"
echo ""
echo "6. Build and run!"
echo ""
echo "Note: You'll need to rebuild the SDK whenever you make changes to it."
echo ""
