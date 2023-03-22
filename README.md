# SetProxy
这是一个用于给 Android 设备设置代理的工具，原理是配置系统的 `http_proxy` 配置项

## 如何使用
配置系统设置需要特殊权限，所以在首次配置代理时需要使用 adb 授予一次权限：
- 下载安装本应用
- 使用 adb 命令授予权限
```shell
adb shell pm grant one.yufz.setproxy android.permission.WRITE_SECURE_SETTINGS
```
- 部分手机可能会限制 adb 授予权限，比如

  - OPPO Color OS 需要在开发者选项中临时打开 `禁止权限监控`
  - 小米 MIUI 需要在开发者选项里临时打开 `USB调试（安全设置）` 
