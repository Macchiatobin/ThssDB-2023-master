package cn.edu.thssdb.exception;

public class AlreadyExistsException extends RuntimeException {
    private String name;
    private int exception_type;
    public static int Database = 1;
    public static int Table = 2;

    public AlreadyExistsException(int exception_type, String name) {
        this.name = name;
        this.exception_type = exception_type;
    }
    @Override
    public String getMessage()
    {
        if (exception_type == Database)
            return "Exception: Database \'" + name + "\' already exists!";
        if (exception_type == Table)
            return "Exception: Table \'" + name + "\' already exists!";

        return "Exception: unknown already exists exception on \'" + name + "\'!";
    }
}
