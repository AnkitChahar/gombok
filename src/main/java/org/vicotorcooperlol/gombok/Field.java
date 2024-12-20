package org.vicotorcooperlol.gombok;


import org.apache.commons.lang3.StringUtils;

public class Field {
    private final String name;
    private final String type;

    public Field(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String toGetter(String structName) {
        String receiver = structName.substring(0, 1).toLowerCase();
        return String.format("func (%s *%s) Get%s() (out %s) {\n" +
                "   if %s == nil {\n" +
                "       return out\n" +
                "   }\n" +
                "   return %s.%s\n" +
                "}\n", receiver, structName, StringUtils.capitalize(this.name), this.type, receiver, receiver, this.name);
    }

    public String toGetterNonPointer(String structName) {
        String receiver = structName.substring(0, 1).toLowerCase();
        return String.format("func (%s *%s) Get%sNonPointer() (out %s) {\n" +
                        "   if %s == nil || %s.%s == nil {\n" +
                        "       return out\n" +
                        "   }\n" +
                        "   return *%s.%s\n" +
                        "}\n", receiver, structName, StringUtils.capitalize(this.name),
                StringUtils.removeStart(this.type, Constants.Pointer), receiver, receiver, this.name, receiver, this.name);
    }

    public String toSetter(String structName) {
        String receiver = structName.substring(0, 1).toLowerCase();
        return String.format("func (%s *%s) Set%s(out %s) {\n" +
                "   if %s == nil {\n" +
                "       return\n" +
                "   }\n" +
                "   %s.%s = out\n" +
                "}\n", receiver, structName, StringUtils.capitalize(this.name), this.type, receiver, receiver, this.name);
    }

    public String toSetterNonPointer(String structName) {
        String receiver = structName.substring(0, 1).toLowerCase();
        return String.format("func (%s *%s) Set%sNonPointer(out %s) {\n" +
                        "   if %s == nil {\n" +
                        "       return\n" +
                        "   }\n" +
                        "   %s.%s = &out\n" +
                        "}\n", receiver, structName, StringUtils.capitalize(this.name),
                StringUtils.removeStart(this.type, Constants.Pointer), receiver, receiver, this.name);
    }

    public String toOutput(String structName) {
        if (this.isPointerType()) {
            return this.toGetter(structName) + "\n" + this.toGetterNonPointer(structName) + "\n" +
                    this.toSetter(structName) + "\n" + this.toSetterNonPointer(structName) + "\n";
        }
        return this.toGetter(structName) + "\n" + this.toSetter(structName) + "\n";
    }

    private boolean isPointerType() {
        return StringUtils.startsWith(this.type, Constants.Pointer);
    }

    public static void main(String[] args) {
        Field f = new Field("myCode", "int32");
        System.out.println(f.toGetter("MyStruct"));
        System.out.println(f.toSetter("MyStruct"));
    }

}
