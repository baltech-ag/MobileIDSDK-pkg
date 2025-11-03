import re
import sys

pbxproj_path = "appnote-swift.xcodeproj/project.pbxproj"

with open(pbxproj_path, 'r') as f:
    content = f.read()

version = sys.argv[1]

# Remove local framework file reference (7555FF82242A565900829871)
content = re.sub(
    r'\t\t7555FF82242A565900829871 /\* MobileIDSdk\.xcframework \*/ = \{[^}]+\};?\n?',
    '',
    content
)

# Add SPM package reference section if not exists
if 'XCRemoteSwiftPackageReference' not in content:
    package_section = f'''
/* Begin XCRemoteSwiftPackageReference section */
		PKG_MOBILEID /* XCRemoteSwiftPackageReference "MobileIDSDK-pkg" */ = {{
			isa = XCRemoteSwiftPackageReference;
			repositoryURL = "https://github.com/baltech-ag/MobileIDSDK-pkg";
			requirement = {{
				kind = exactVersion;
				version = {version};
			}};
		}};
/* End XCRemoteSwiftPackageReference section */
'''

    product_section = '''
/* Begin XCSwiftPackageProductDependency section */
		PKG_MOBILEID_PRODUCT /* MobileIDSdk */ = {{
			isa = XCSwiftPackageProductDependency;
			package = PKG_MOBILEID;
			productName = MobileIDSdk;
		}};
/* End XCSwiftPackageProductDependency section */
'''

    # Insert before end of PBXProject section
    content = re.sub(
        r'(/\* End PBXProject section \*/)',
        package_section + product_section + r'\1',
        content
    )

# Update framework buildFile references to use SPM product
content = re.sub(
    r'17495DE58D31D0DDEA84AC27 /\* MobileIDSdk\.xcframework in Frameworks \*/ = \{isa = PBXBuildFile; fileRef = 7555FF82242A565900829871 /\* MobileIDSdk\.xcframework \*/; \};',
    '17495DE58D31D0DDEA84AC27 /* MobileIDSdk in Frameworks */ = {isa = PBXBuildFile; productRef = PKG_MOBILEID_PRODUCT /* MobileIDSdk */; };',
    content
)

content = re.sub(
    r'17495DE58D31D0DDEA84AC28 /\* MobileIDSdk\.xcframework in Embed Frameworks \*/ = \{isa = PBXBuildFile; fileRef = 7555FF82242A565900829871 /\* MobileIDSdk\.xcframework \*/;[^}]+\};',
    '',
    content
)

# Remove from Frameworks group
content = re.sub(
    r'\t\t\t\t7555FF82242A565900829871 /\* MobileIDSdk\.xcframework \*/,?\n?',
    '',
    content
)

# Remove FRAMEWORK_SEARCH_PATHS with relative paths
content = re.sub(
    r'FRAMEWORK_SEARCH_PATHS = \(\n[^)]*\$\(PROJECT_DIR\)/\.\./\.\./sdk/build/XCFrameworks/debug[^)]*\);',
    'FRAMEWORK_SEARCH_PATHS = ("$(inherited)",);',
    content
)

# Remove OTHER_LDFLAGS with relative paths
content = re.sub(
    r'OTHER_LDFLAGS = \(\n[^)]*\$\(PROJECT_DIR\)/\.\./\.\./sdk/build/[^)]*\);',
    'OTHER_LDFLAGS = ("$(inherited)",);',
    content
)

# Add package dependencies to project
content = re.sub(
    r'(productRefGroup = [^;]+;)\n',
    r'\1\n\t\t\t\tpackageReferences = (\n\t\t\t\t\tPKG_MOBILEID /* XCRemoteSwiftPackageReference "MobileIDSDK-pkg" */,\n\t\t\t\t);\n',
    content
)

with open(pbxproj_path, 'w') as f:
    f.write(content)

print(f"✓ Swift appnote configured to use SPM with version {version}")
