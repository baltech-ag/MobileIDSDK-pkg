import Foundation
import CryptoKit
import CommonCrypto

// MARK: - SHA256

@_cdecl("swift_cryptokit_sha256")
public func sha256(_ data: NSData) -> NSData {
    let dataSwift = data as Data
    let hash = SHA256.hash(data: dataSwift)
    return Data(hash) as NSData
}

// MARK: - AES-GCM Encryption

@_cdecl("swift_cryptokit_aesGcmEncrypt")
public func aesGcmEncrypt(
    plain: NSData,
    key: NSData,
    iv: NSData,
    aad: NSData
) -> NSDictionary? {
    do {
        let plainData = plain as Data
        let keyData = key as Data
        let ivData = iv as Data
        let aadData = aad as Data

        let symmetricKey = SymmetricKey(data: keyData)
        let nonce = try AES.GCM.Nonce(data: ivData)

        let sealedBox = try AES.GCM.seal(
            plainData,
            using: symmetricKey,
            nonce: nonce,
            authenticating: aadData
        )

        // Extract ciphertext and tag (GMAC)
        let ciphertext = sealedBox.ciphertext
        let tag = sealedBox.tag

        return [
            "encrypted": ciphertext as NSData,
            "gmac": tag as NSData
        ] as NSDictionary
    } catch {
        NSLog("AES-GCM encryption failed: \(error)")
        return nil
    }
}

// MARK: - AES-GCM Decryption

@_cdecl("swift_cryptokit_aesGcmDecrypt")
public func aesGcmDecrypt(
    encrypted: NSData,
    gmac: NSData,
    key: NSData,
    iv: NSData,
    aad: NSData
) -> NSData? {
    do {
        let encryptedData = encrypted as Data
        let gmacData = gmac as Data
        let keyData = key as Data
        let ivData = iv as Data
        let aadData = aad as Data

        let symmetricKey = SymmetricKey(data: keyData)
        let nonce = try AES.GCM.Nonce(data: ivData)

        // Reconstruct the sealed box from ciphertext and tag
        let sealedBox = try AES.GCM.SealedBox(
            nonce: nonce,
            ciphertext: encryptedData,
            tag: gmacData
        )

        let plaintext = try AES.GCM.open(
            sealedBox,
            using: symmetricKey,
            authenticating: aadData
        )

        return plaintext as NSData
    } catch {
        NSLog("AES-GCM decryption failed: \(error)")
        return nil
    }
}

// MARK: - AES-CMAC

@_cdecl("swift_cryptokit_aesCmac")
public func aesCmac(
    data: NSData,
    key: NSData
) -> NSData? {
    let dataSwift = data as Data
    let keySwift = key as Data

    guard let mac = computeCMAC(data: dataSwift, key: keySwift) else {
        NSLog("AES-CMAC computation failed")
        return nil
    }

    return mac as NSData
}

// Helper function to compute AES-CMAC using CommonCrypto
private func computeCMAC(data: Data, key: Data) -> Data? {
    let blockSize = kCCBlockSizeAES128
    var subkey = Data(count: blockSize)

    // Step 1: Generate L by encrypting zero block
    var zeroBlock = Data(count: blockSize)
    var outputLength = 0

    let status1 = zeroBlock.withUnsafeBytes { zeroPtr in
        subkey.withUnsafeMutableBytes { subkeyPtr in
            key.withUnsafeBytes { keyPtr in
                CCCrypt(
                    CCOperation(kCCEncrypt),
                    CCAlgorithm(kCCAlgorithmAES),
                    CCOptions(0),
                    keyPtr.baseAddress,
                    key.count,
                    nil,
                    zeroPtr.baseAddress,
                    blockSize,
                    subkeyPtr.baseAddress,
                    blockSize,
                    &outputLength
                )
            }
        }
    }

    guard status1 == kCCSuccess else {
        NSLog("Failed to generate CMAC subkey: \(status1)")
        return nil
    }

    // Step 2: Generate K1 by left-shifting L
    var k1 = leftShift(subkey)
    if (subkey[0] & 0x80) != 0 {
        k1[blockSize - 1] ^= 0x87
    }

    // Step 3: Determine if padding is needed and select K1 or K2
    var processedData: Data
    var lastBlockKey: Data

    if data.isEmpty || data.count % blockSize != 0 {
        // Need padding - use K2
        var k2 = leftShift(k1)
        if (k1[0] & 0x80) != 0 {
            k2[blockSize - 1] ^= 0x87
        }
        processedData = padData(data, blockSize: blockSize)
        lastBlockKey = k2
    } else {
        // No padding - use K1
        processedData = data
        lastBlockKey = k1
    }

    // XOR last block with the appropriate key
    let lastBlockStart = processedData.count - blockSize
    for i in 0..<blockSize {
        processedData[lastBlockStart + i] ^= lastBlockKey[i]
    }

    // Step 4: Encrypt with AES-CBC using zero IV
    var outputBuffer = Data(count: processedData.count)
    var zeroIV = Data(count: blockSize)
    outputLength = 0

    let status2 = processedData.withUnsafeBytes { dataPtr in
        outputBuffer.withUnsafeMutableBytes { outputPtr in
            key.withUnsafeBytes { keyPtr in
                zeroIV.withUnsafeBytes { ivPtr in
                    CCCrypt(
                        CCOperation(kCCEncrypt),
                        CCAlgorithm(kCCAlgorithmAES),
                        CCOptions(0),
                        keyPtr.baseAddress,
                        key.count,
                        ivPtr.baseAddress,
                        dataPtr.baseAddress,
                        processedData.count,
                        outputPtr.baseAddress,
                        processedData.count,
                        &outputLength
                    )
                }
            }
        }
    }

    guard status2 == kCCSuccess else {
        NSLog("Failed to compute CMAC: \(status2)")
        return nil
    }

    // Return the last block as the MAC
    return outputBuffer.suffix(blockSize)
}

// Helper: Left shift operation for CMAC
private func leftShift(_ input: Data) -> Data {
    var output = Data(count: input.count)
    var carry: UInt8 = 0

    for i in (0..<input.count).reversed() {
        let b = input[i]
        output[i] = (b << 1) | carry
        carry = (b >> 7) & 1
    }

    return output
}

// Helper: Pad data for CMAC
private func padData(_ data: Data, blockSize: Int) -> Data {
    var padded = data
    padded.append(0x80) // First padding byte

    let paddingNeeded = blockSize - (padded.count % blockSize)
    if paddingNeeded < blockSize {
        padded.append(Data(count: paddingNeeded))
    }

    return padded
}