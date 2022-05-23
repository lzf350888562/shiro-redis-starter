package xyz.lzf.ext.shiro;

import org.apache.shiro.session.mgt.SimpleSession;

import java.io.*;
import java.util.Date;

public class SimpleSessionTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        SimpleSession simpleSession = new SimpleSession();
        simpleSession.setAttribute("a","b");
        simpleSession.setId(123124L);
        simpleSession.setExpired(true);
        simpleSession.setHost("127.0.0.1");
        simpleSession.setLastAccessTime(new Date());
        FileOutputStream fileOutputStream = new FileOutputStream("session.txt");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(simpleSession);
        System.out.println(simpleSession);
        System.out.println("------------");
        FileInputStream fileInputStream = new FileInputStream("session.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        SimpleSession o = (SimpleSession)objectInputStream.readObject();
        System.out.println(o);
        System.out.println(o.getLastAccessTime());
        System.out.println(o.getAttributes());

    }

    public static class tr implements Serializable{
        private String name;
        private transient String addr;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddr() {
            return addr;
        }

        public void setAddr(String addr) {
            this.addr = addr;
        }
    }
}
