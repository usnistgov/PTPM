/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package humantrackingperformancemetrics;

/*
Source code follows: adapted from Exec.java found in jsch examples
By vaidya.anand at gmail.com

anand@aries5672:~/workspace/samplejsch$ cat samplejsch.java
*/ 
//import com.jcraft.jsch.*;
import java.io.*;

public class samplejsch {

  public static void main(String[] args) {

//    try{
//      JSch jsch=new JSch();
//
//      String host="shackle@answer3.mxnet.mel.nist.gov"; //System.getProperty("user.name") + "@localhost";
//      if(args.length>0){
//        host=args[0];
//      }
//      String user=host.substring(0, host.indexOf('@'));
//
//      host=host.substring(host.indexOf('@')+1);
//      Session session=jsch.getSession(user, host, 22);
//
//      
//      //jsch.setKnownHosts("/home/anand/.ssh/known_hosts");
//      //jsch.addIdentity("/home/anand/.ssh/id_rsa");
//
//      // If two machines have SSH passwordless logins setup, the following line is not needed:
//      session.setPassword("#EDCvfr45tgb");
//        System.out.println(session.getConfig("StrictHostKeyChecking"));
//      session.setConfig("StrictHostKeyChecking", "no");
//      session.connect();
//
//      String command="ps -ef;date;hostname";
//      // command=args[1];
//
//      Channel channel=session.openChannel("exec");
//      ((ChannelExec)channel).setCommand(command);
//
//      //channel.setInputStream(System.in);
//      channel.setInputStream(null);
//
//      ((ChannelExec)channel).setErrStream(System.err);
//
//      InputStream in=channel.getInputStream();
//
//      channel.connect();
//      byte[] tmp=new byte[1024];
//      while(true){
//        while(in.available()>0){
//          int i=in.read(tmp, 0, 1024);
//          if(i<0)break;
//          System.out.print(new String(tmp, 0, i));
//        }
//        if(channel.isClosed()){
//          System.out.println("exit-status: "+channel.getExitStatus());
//          break;
//        }
//        try{Thread.sleep(1000);}catch(Exception ee) {ee.printStackTrace();}
//      }
//      channel.disconnect();
//      session.disconnect();
//    }
//    catch(Exception e){
//      e.printStackTrace();
//    }
  }       //end main
}       //end class