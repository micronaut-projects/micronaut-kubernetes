package io.micronaut.kubernetes.test

trait KubectlCommands {

    List<String> getEndpoints(){
        return getProcessOutput("kubectl get endpoints | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    List<String> getServices(){
        return getProcessOutput("kubectl get services | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    String getClusterIp() {
        return getProcessOutput("kubectl get service example-service | awk 'FNR > 1 { print \$3 }'").trim()
    }

    List<String> getIps() {
        return getProcessOutput("kubectl get endpoints example-service | awk 'FNR > 1 { print \$2 }'")
                .split('\\,')
                .collect { it.split(':').first() }
    }

    List<String> getConfigMaps() {
        return getProcessOutput("kubectl get configmaps | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    String getProcessOutput(String command) {
        Process p = ['bash', '-c', command].execute()
        p.waitFor()
        return p.text
    }
}