package pub.devrel.easypermissions;

import android.Manifest;
import android.content.Context;

import androidx.collection.SimpleArrayMap;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class PermissionRegistery {
    private static SimpleArrayMap<String, String> permissions;

    static {
        put(Manifest.permission.CAMERA, "相机");
        put(Manifest.permission.WRITE_EXTERNAL_STORAGE, "文件存储");
        put(Manifest.permission.READ_PHONE_STATE, "设备状态");
        put(Manifest.permission.ACCESS_WIFI_STATE, "wifi状态");
        put(Manifest.permission.ACCESS_COARSE_LOCATION, "位置信息");
    }

    /**
     * 添加权限
     *
     * @param permission 权限
     * @param desc       权限描述
     */
    public static void put(String permission, String desc) {
        if (permissions == null) {
            permissions = new SimpleArrayMap<>(50);
        }
        permissions.put(permission, desc);
    }

    /**
     * 获取权限描述
     *
     * @param permission
     * @return
     */
    public static String getPermissionDesc(String permission) {
        if (permissions != null && permissions.containsKey(permission)) {
            return permissions.get(permission);
        } else {
            return "未知权限";
        }
    }

    /**
     * 获取还没有的权限
     *
     * @return
     */
    public static Pair<List<String>, List<String>> getDenyPermissions(Context context) {
        if (permissions != null && !permissions.isEmpty()) {
            int size = permissions.size();
            List<String> keys = new ArrayList();
            List<String> descs = new ArrayList();
            String key, desc;
            for (int i = 0; i < size; i++) {
                key = permissions.keyAt(i);
                desc = permissions.valueAt(i);
                if (!EasyPermissions.hasPermissions(context, key)) {
                    keys.add(key);
                    descs.add(desc);
                }
            }
            return new Pair<List<String>, List<String>>(keys, descs);
        }
        return null;
    }

    public static boolean hasPermission(Context context, String permissionKey) {
        return EasyPermissions.hasPermissions(context, permissionKey);
    }
}
