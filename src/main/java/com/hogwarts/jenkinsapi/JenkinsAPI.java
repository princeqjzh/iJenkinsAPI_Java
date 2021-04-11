package com.hogwarts.jenkinsapi;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.JobWithDetails;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public class JenkinsAPI {
    private JenkinsServer jenkinsServer;

    public JenkinsAPI() throws URISyntaxException{
        String username = "qa";
        String password = "123456";
        String jenkinsURL = "http://localhost:8081";
        jenkinsServer = new JenkinsServer(new URI(jenkinsURL), username, password);
    }

    public JobWithDetails getJob(String jobName) throws IOException{
        return jenkinsServer.getJob(jobName);
    }

    public int getLastBuildNumber(String jobName) throws IOException {
        return jenkinsServer.getJob(jobName).getLastBuild().getNumber();
    }

    public boolean isBuildRunning(String jobName, int buildNumber) throws IOException {
        return jenkinsServer.getJob(jobName).getBuildByNumber(buildNumber).details().isBuilding();
    }

    public String getBuildResult(String jobName, int buildNumber) throws IOException {
        return jenkinsServer.getJob(jobName).getBuildByNumber(buildNumber).details().getResult().name();
    }

    public static void wait(int second){
        try {
            Thread.sleep(second * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        JenkinsAPI jenkinsAPI = new JenkinsAPI();
        String jobName = "TestEmail";
        int timeout = 60; //60秒超时

        //获取当前lastBuildNumber
        int oldBuildNumber = jenkinsAPI.getLastBuildNumber(jobName);

        //启动任务
        jenkinsAPI.getJob(jobName).build();

        //获取最新任务编号
        int newBuildNumber = jenkinsAPI.getLastBuildNumber(jobName);
        long start = System.currentTimeMillis();
        while(!(newBuildNumber > oldBuildNumber)){
            JenkinsAPI.wait(2);
            newBuildNumber = jenkinsAPI.getLastBuildNumber(jobName);

            //判断超时
            long end = System.currentTimeMillis();
            if(end - start > timeout * 1000){
                throw new TimeoutException(timeout + "秒超时");
            }
        }
        System.out.println("新任务编号：" + newBuildNumber);

        //等待任务执行完毕
        boolean buildingStatus = jenkinsAPI.isBuildRunning(jobName, newBuildNumber);
        start = System.currentTimeMillis();
        while (buildingStatus){
            JenkinsAPI.wait(2);
            System.out.println("任务" + jobName + "正在运行 ...");
            buildingStatus = jenkinsAPI.isBuildRunning(jobName, newBuildNumber);

            //判断超时
            long end = System.currentTimeMillis();
            if(end - start > timeout * 1000){
                throw new TimeoutException(timeout + "秒超时");
            }
        }

        //任务运行完毕，获取任务结果
        String buildResult = jenkinsAPI.getBuildResult(jobName, newBuildNumber);
        System.out.println("任务" + jobName + "运行完毕，最新任务编号：" + newBuildNumber + ", 运行结果：" + buildResult);
    }
}
