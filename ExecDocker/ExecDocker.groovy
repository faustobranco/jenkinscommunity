package devopsdb.docker
@Grapes([@Grab(group='com.github.docker-java', module='docker-java', version='3.3.6'),
         @Grab(group='com.github.docker-java', module='docker-java-transport-okhttp', version='3.3.6', scope='test')])

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import devopsdb.log.Logger
import devopsdb.log.LogFormatterConstants
import devopsdb.log.LogFormatter

@Singleton(strict = false)
class ExecDocker implements Serializable {

    def obj_Pipeline_Context
    def ExecDocker(obj_Context) {
        obj_Pipeline_Context = obj_Context
    }
    def run_Command()
    {
        String str_Docker_URI = 'tcp://172.21.5.70:2375';
        String str_Container_Name = 'test_python';
        def lst_Command = ["/bin/bash", "-c", "counter=0; until [ \$counter -gt 5 ]; do echo \$(date '+%d/%m/%Y %H:%M:%S'); ((counter++)); sleep 1; done"]

        LogFormatter obj_Formatter = new LogFormatter(LogFormatterConstants.const_Info, 'ExecDocker', false, '')
        Logger obj_Log = new Logger(obj_Pipeline_Context, obj_Formatter)

        StringBuilder std_out = new StringBuilder();
        StringBuilder std_err = new StringBuilder();
        Boolean std_complete = false;

        obj_Log.info('Initializing obj_dockerClientConfig')
        DockerClientConfig obj_dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(str_Docker_URI)
                .build();

        obj_Log.info('Initializing obj_httpClient')
        DockerHttpClient obj_httpClient = new OkDockerHttpClient.Builder()
                .dockerHost(obj_dockerClientConfig.getDockerHost())
                .sslConfig(obj_dockerClientConfig.getSSLConfig())
                .build();

        obj_Log.info('Initializing obj_dockerClient')
        DockerClient obj_dockerClient = DockerClientBuilder.getInstance(obj_dockerClientConfig)
                .withDockerHttpClient(obj_httpClient)
                .build();

        obj_Log.info('Initializing obj_container')
        InspectContainerResponse obj_container = obj_dockerClient.inspectContainerCmd(str_Container_Name).exec();

        try {
            obj_Log.info('Initializing obj_cmd_exec')
            def obj_cmd_exec = obj_dockerClient
                    .execCreateCmd(obj_container.getId())
                    .withCmd(lst_Command.join(' '))
                    .withPrivileged(true)
                    .withAttachStdout(true)
                    .exec().getId()
            obj_Log.info('Initializing obj_Return')
            def obj_Return = obj_dockerClient
                    .execStartCmd(obj_cmd_exec)
                    .withTty(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        void onNext(Frame object) {
                            def str_tmp_result = new String(object.getPayload()).trim();
                            obj_Log.info(str_tmp_result)
                            std_out.append(str_tmp_result);
                            super.onNext(object);
                        }
                        @Override
                        void onError(Throwable throwable) {
                            def str_tmp_result = new String(throwable as String).trim();
                            std_err.append(str_tmp_result);
                            super.onError(throwable);
                        }
                        @Override
                        void onComplete() {
                            std_complete=true;
                            super.onComplete();
                        }
                    })
                    .awaitCompletion()
            obj_Log.info("End")
        }
        catch (Exception obj_Exception) {
            obj_Log.info("Can't exec Docker command" + obj_Exception.toString() + " - Line: " + obj_Exception.getStackTrace().toString());
        }
    }

}
