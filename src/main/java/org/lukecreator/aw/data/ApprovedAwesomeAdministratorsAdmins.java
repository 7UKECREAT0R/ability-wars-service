package org.lukecreator.aw.data;

public class ApprovedAwesomeAdministratorsAdmins {
    public static final long[] ADMINS = {
            214183045278728202L, // luke
            348136128932610058L, // checken
            367618656613564418L, // ducky
            239526347972673537L  // yucky
    };

    public static boolean isApprovedAwesomeAdministratorAdmin(long id) {
        for (long admin : ADMINS) {
            if (admin == id) {
                return true;
            }
        }
        return false;
    }
}
