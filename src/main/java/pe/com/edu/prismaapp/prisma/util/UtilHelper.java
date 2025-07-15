package pe.com.edu.prismaapp.prisma.util;

import java.util.Date;

public class UtilHelper {

    public static boolean validateCurrent(Date startDate, Date endDate){
        Date date = new Date();
        return startDate.before(date) && endDate.after(date);
    }
}
