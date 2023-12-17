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
        return String.format("func (receiver *%s) Get%s() (v %s) {\n" +
                "   if receiver == nil {\n" +
                "       return v\n" +
                "   }\n" +
                "   return receiver.%s\n" +
                "}\n", structName, StringUtils.capitalize(this.name), this.type, this.name);
    }

    public String toGetterNonPointer(String structName) {
        return String.format("func (receiver *%s) Get%sNonPointer() (v %s) {\n" +
                        "   if receiver == nil || receiver.%s == nil {\n" +
                        "       return v\n" +
                        "   }\n" +
                        "   return *receiver.%s\n" +
                        "}\n", structName, StringUtils.capitalize(this.name),
                StringUtils.removeStart(this.type, Constants.Pointer), this.name, this.name);
    }

    public String toSetter(String structName) {
        return String.format("func (receiver *%s) Set%s(v %s) {\n" +
                "   if receiver == nil {\n" +
                "       return\n" +
                "   }\n" +
                "   receiver.%s = v\n" +
                "}\n", structName, StringUtils.capitalize(this.name), this.type, this.name);
    }

    public String toSetterNonPointer(String structName) {
        return String.format("func (receiver *%s) Set%sNonPointer(v %s) {\n" +
                        "   if receiver == nil {\n" +
                        "       return\n" +
                        "   }\n" +
                        "   receiver.%s = &v\n" +
                        "}\n", structName, StringUtils.capitalize(this.name),
                StringUtils.removeStart(this.type, Constants.Pointer), this.name);
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
