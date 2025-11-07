#import <Foundation/Foundation.h>
#import "CryptoKitBridgeWrapper.h"

// External Swift functions
extern NSData* _Nonnull swift_cryptokit_sha256(NSData* _Nonnull data);
extern NSDictionary* _Nullable swift_cryptokit_aesGcmEncrypt(NSData* _Nonnull plain, NSData* _Nonnull key, NSData* _Nonnull iv, NSData* _Nonnull aad);
extern NSData* _Nullable swift_cryptokit_aesGcmDecrypt(NSData* _Nonnull encrypted, NSData* _Nonnull gmac, NSData* _Nonnull key, NSData* _Nonnull iv, NSData* _Nonnull aad);
extern NSData* _Nullable swift_cryptokit_aesCmac(NSData* _Nonnull data, NSData* _Nonnull key);

@implementation CryptoKitBridgeWrapper

+ (NSData *)sha256:(NSData *)data {
    return swift_cryptokit_sha256(data);
}

+ (NSDictionary *)aesGcmEncryptPlain:(NSData *)plain key:(NSData *)key iv:(NSData *)iv aad:(NSData *)aad {
    return swift_cryptokit_aesGcmEncrypt(plain, key, iv, aad);
}

+ (NSData *)aesGcmDecryptEncrypted:(NSData *)encrypted gmac:(NSData *)gmac key:(NSData *)key iv:(NSData *)iv aad:(NSData *)aad {
    return swift_cryptokit_aesGcmDecrypt(encrypted, gmac, key, iv, aad);
}

+ (NSData *)aesCmacData:(NSData *)data key:(NSData *)key {
    return swift_cryptokit_aesCmac(data, key);
}

@end