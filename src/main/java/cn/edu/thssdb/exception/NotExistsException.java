package cn.edu.thssdb.exception;

public class NotExistsException extends RuntimeException{
    private String name;
    private int exception_type;
    public static int Database = 1;
    public static int Table = 2;

    public NotExistsException(int exception_type, String name) {
        this.name = name;
        this.exception_type = exception_type;
    }
    @Override
    public String getMessage()
    {
        if (exception_type == Database)
            return "Exception: Database \'" + name + "\' does not exist!";
        if (exception_type == Table)
            return "Exception: Table \'" + name + "\' does not exist!";

        return "Exception: unknown not exists exception on \'" + name + "\'!";
    }
}
