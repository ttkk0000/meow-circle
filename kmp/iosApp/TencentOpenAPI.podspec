Pod::Spec.new do |s|
  s.name = 'TencentOpenAPI'
  s.version = '3.6.20'
  s.summary = 'Pinned official QQ Connect SDK, distributed by Tencent.'
  s.homepage = 'https://wiki.connect.qq.com/sdk下载/'
  s.license = { :type => 'Commercial', :text => 'Tencent QQ Connect developer agreement applies. https://wiki.connect.qq.com/开发者协议/' }
  s.author = { 'Tencent' => 'https://connect.qq.com/' }
  s.platform = :ios, '17.0'
  s.source = { :http => 'https://tangram-1251316161.file.myqcloud.com/files/20260402/725f52c913f9bdc7c41bee57d2923c05.zip', :sha256 => '77c4c9e4b0f064c5a304e00d036280f6cf47d43ae0fc24ab99785d4659ce6198' }
  s.prepare_command = <<-CMD
    mkdir -p mnd-sdk/TencentOpenAPI.xcframework
    if [ -f 'iOS_3.6.20_lite/TencentOpenAPI_xcframework_Lite.zip' ]; then
      unzip -q 'iOS_3.6.20_lite/TencentOpenAPI_xcframework_Lite.zip' -d mnd-sdk/TencentOpenAPI.xcframework
    else
      unzip -q 'TencentOpenAPI_xcframework_Lite.zip' -d mnd-sdk/TencentOpenAPI.xcframework
    fi
  CMD
  s.vendored_frameworks = 'mnd-sdk/TencentOpenAPI.xcframework'
  s.frameworks = 'Security', 'SystemConfiguration', 'CoreGraphics', 'QuartzCore', 'WebKit'
  s.libraries = 'z', 'c++'
  s.static_framework = true
end
