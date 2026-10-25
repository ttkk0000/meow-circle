#import <UIKit/UIKit.h>
#import <WechatOpenSDK/WXApi.h>
#import <TencentOpenAPI/TencentOAuth.h>

NS_ASSUME_NONNULL_BEGIN
BOOL MndWechatRegister(NSString *appID, NSString *universalLink);
void MndWechatAuthorize(UIViewController *controller, NSString *state, id<WXApiDelegate> delegate, void (^completion)(BOOL));
BOOL MndWechatHandleURL(NSURL *url, id<WXApiDelegate> delegate);
BOOL MndWechatHandleUniversal(NSUserActivity *activity, id<WXApiDelegate> delegate);
TencentOAuth *MndQqSetup(NSString *appID, NSString *universalLink, id<TencentSessionDelegate> delegate);
BOOL MndQqAuthorize(TencentOAuth *client);
NSString * _Nullable MndQqCode(TencentOAuth *client);
void MndQqClear(TencentOAuth *client, id<TencentSessionDelegate> delegate);
BOOL MndQqHandleURL(NSURL *url);
BOOL MndQqHandleUniversal(NSURL *url);
NS_ASSUME_NONNULL_END
