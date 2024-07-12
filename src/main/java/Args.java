package main.java;

public class Args {
    private final String[] args;

    public Args(final String[] args){
        this.args = args;
    }

    public boolean contains(final String arg){
        for(final String s : args){
            if(s.equals(arg))
                return true;
        }
        return false;
    }

    public String get(final String arg){
        boolean found = false;
        for(final String s : args){
            if(found)
                return s;
            found = s.equals(arg);
        }
        return null;
    }
}
