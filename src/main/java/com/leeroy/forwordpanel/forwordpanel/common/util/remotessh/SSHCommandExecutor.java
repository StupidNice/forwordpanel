package com.leeroy.forwordpanel.forwordpanel.common.util.remotessh;

import com.jcraft.jsch.*;
import com.leeroy.forwordpanel.forwordpanel.model.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Vector;

/**
 * This class provide interface to execute command on remote Linux.
 */
@Slf4j
public class SSHCommandExecutor {
    private String ipAddress;

    private String username;

    private String password;

    private String privateKeyPath;

    public static final int DEFAULT_SSH_PORT = 34204;

    private Vector<String> stdout;

    public SSHCommandExecutor(final String ipAddress, final String username, final String password) {
        this.ipAddress = ipAddress;
        this.username = username;
        this.password = password;
        stdout = new Vector<>();
    }

    public SSHCommandExecutor(Server server, final String username, final String privateKeyPath) {
        this.ipAddress = server.getHost();
        this.username = username;
        this.privateKeyPath = privateKeyPath;
        stdout = new Vector<>();
    }

    public int execute(final String... commandList) {
        stdout.clear();
        int returnCode = 0;
        JSch jsch = new JSch();
        try {
            jsch.addIdentity(privateKeyPath);
            MyUserInfo userInfo = new MyUserInfo();
            // Create and connect session.
            Session session = jsch.getSession(username, ipAddress, DEFAULT_SSH_PORT);
            session.setUserInfo(userInfo);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            for (String command : commandList) {
                // Create and connect channel.
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                BufferedReader input = new BufferedReader(new InputStreamReader(channel
                        .getInputStream()));
                channel.connect();
                log.info("The remote command is: {}", command);

                // Get the output of remote command.
                String line;
                while ((line = input.readLine()) != null) {
                    stdout.add(line);
                }
                input.close();
                // Get the return code only after the channel is closed.
                if (channel.isClosed()) {
                    returnCode = channel.getExitStatus();
                }
                // Disconnect the channel and session.
                channel.disconnect();
            }

            session.disconnect();
        } catch (JSchException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("shell result: {}", StringUtils.join(stdout));
        return returnCode;
    }

    public Vector<String> getResultSet() {
        return stdout;
    }

    public String getResult() {
        return CollectionUtils.isEmpty(stdout) ? "" : stdout.get(0);
    }

    public static void main(final String[] args) {
        SSHCommandExecutor sshExecutor = new SSHCommandExecutor("120.241.154.4", "root", "28L8CegNk9");
        long stsart = System.currentTimeMillis();
        sshExecutor.execute("netstat -tunlp", "netstat -tunlp", "netstat -tunlp", "netstat -tunlp");
        Vector<String> stdout = sshExecutor.getResultSet();
        for (String str : stdout) {
            System.out.println(str);
        }
        System.out.println(System.currentTimeMillis() - stsart);
    }
}
