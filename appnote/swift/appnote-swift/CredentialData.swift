import Foundation
import MobileIDSdk

struct CredentialData: Codable, Identifiable, Equatable {
    let projectKeyHex: String  // 32 hex characters (16 bytes)
    let credentialId: String   // ASCII string

    var id: String { credentialId }
}

extension String {
    func isValidHex() -> Bool {
        return self.count == 32 && self.allSatisfy { $0.isHexDigit }
    }

    func hexToByteArray() throws -> Data {
        guard self.count == 32 else {
            throw HexConversionError.invalidLength
        }
        guard self.allSatisfy({ $0.isHexDigit }) else {
            throw HexConversionError.invalidCharacters
        }

        var data = Data()
        var index = self.startIndex

        while index < self.endIndex {
            let nextIndex = self.index(index, offsetBy: 2)
            let byteString = self[index..<nextIndex]
            if let byte = UInt8(byteString, radix: 16) {
                data.append(byte)
            } else {
                throw HexConversionError.conversionFailed
            }
            index = nextIndex
        }

        return data
    }
}

enum HexConversionError: Error {
    case invalidLength
    case invalidCharacters
    case conversionFailed
}

extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let bytes = [UInt8](self)
        return KotlinByteArray(size: Int32(bytes.count)) { index in
            return KotlinByte(value: Int8(bitPattern: bytes[Int(truncating: index)]))
        }
    }
}
