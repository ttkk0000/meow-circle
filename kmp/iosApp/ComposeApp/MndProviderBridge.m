#import "MndProviderBridge.h"

// 只翻译官方ObjC选择子，业务、账号选择和凭据核验仍由KMP及服务端管理。
BOOL MndWechatRegister(NSString *appID, NSString *universalLink) {
    return [WXApi registerApp:appID universalLink:universalLink];
}
void MndWechatAuthorize(UIViewController *controller, NSString *state, id<WXApiDelegate> delegate, void (^completion)(BOOL)) {
    SendAuthReq *request = [SendAuthReq new];
    request.scope = @"snsapi_userinfo";
    request.state = state;
    [WXApi sendAuthReq:request viewController:controller delegate:delegate completion:completion];
}
BOOL MndWechatHandleURL(NSURL *url, id<WXApiDelegate> delegate) { return [WXApi handleOpenURL:url delegate:delegate]; }
BOOL MndWechatHandleUniversal(NSUserActivity *activity, id<WXApiDelegate> delegate) { return [WXApi handleOpenUniversalLink:activity delegate:delegate]; }
TencentOAuth *MndQqSetup(NSString *appID, NSString *universalLink, id<TencentSessionDelegate> delegate) {
    [TencentOAuth setIsUserAgreedAuthorization:YES];
    TencentOAuth *client = [TencentOAuth sharedInstance];
    [client setupAppId:appID enableUniveralLink:YES universalLink:universalLink delegate:delegate];
    client.authMode = kAuthModeServerSideCode;
    client.authShareType = AuthShareType_QQ;
    return client;
}
BOOL MndQqAuthorize(TencentOAuth *client) { return [client authorize:@[@"get_simple_userinfo"]]; }
NSString *MndQqCode(TencentOAuth *client) { return [client getServerSideCode]; }
void MndQqClear(TencentOAuth *client, id<TencentSessionDelegate> delegate) { [client logout:delegate]; }
BOOL MndQqHandleURL(NSURL *url) { return [TencentOAuth CanHandleOpenURL:url] && [TencentOAuth HandleOpenURL:url]; }
BOOL MndQqHandleUniversal(NSURL *url) { return [TencentOAuth CanHandleUniversalLink:url] && [TencentOAuth HandleUniversalLink:url]; }
