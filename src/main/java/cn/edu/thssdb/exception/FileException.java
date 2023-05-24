package cn.edu.thssdb.exception;

public class FileException extends RuntimeException {
    private String file_name;
    private int exception_type;
    public static int FileNotFound = 0;
    public static int Create = 1;
    public static int Delete = 2;
    public static int Open = 3;
    public static int ReadWrite = 4;

    public FileException(int exception_type, String file_name) {
        this.file_name = file_name;
        this.exception_type = exception_type;
    }
    @Override
    public String getMessage()
    {
        if (exception_type == FileNotFound)
            return "Exception: file \'" + file_name + "\' not found!";
        if (exception_type == Create)
            return "Exception: failed to create file \'" + file_name + "\'!";
        if (exception_type == Delete)
            return "Exception: failed to delete file \'" + file_name + "\'!";
        if (exception_type == Open)
            return "Exception: failed to open file \'" + file_name + "\'!";
        if (exception_type == ReadWrite)
            return "Exception: failed to read/write file \'" + file_name + "\'!";

        return "Exception: unknown file exception on \'" + file_name + "\'!";
    }
}
