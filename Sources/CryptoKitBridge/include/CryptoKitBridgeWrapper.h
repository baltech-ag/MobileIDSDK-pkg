#import <Foundation/Foundation.h>

@interface CryptoKitBridgeWrapper : NSObject

+ (NSData * _Nonnull)sha256:(NSData * _Nonnull)data;
+ (NSDictionary * _Nullable)aesGcmEncryptPlain:(NSData * _Nonnull)plain key:(NSData * _Nonnull)key iv:(NSData * _Nonnull)iv aad:(NSData * _Nonnull)aad;
+ (NSData * _Nullable)aesGcmDecryptEncrypted:(NSData * _Nonnull)encrypted gmac:(NSData * _Nonnull)gmac key:(NSData * _Nonnull)key iv:(NSData * _Nonnull)iv aad:(NSData * _Nonnull)aad;
+ (NSData * _Nullable)aesCmacData:(NSData * _Nonnull)data key:(NSData * _Nonnull)key;

@end