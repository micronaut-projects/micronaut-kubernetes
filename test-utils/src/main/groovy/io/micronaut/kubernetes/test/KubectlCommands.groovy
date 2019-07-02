package io.micronaut.kubernetes.test

import groovy.transform.Memoized

trait KubectlCommands {

    @Memoized
    List<String> getEndpoints(){
        return getProcessOutput("kubectl get endpoints | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    List<String> getServices(){
        return getProcessOutput("kubectl get services | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    String getClusterIp() {
        return getProcessOutput("kubectl get service example-service | awk 'FNR > 1 { print \$3 }'").trim()
    }

    @Memoized
    List<String> getIps() {
        return getProcessOutput("kubectl get endpoints example-service | awk 'FNR > 1 { print \$2 }'")
                .split('\\,')
                .collect { it.split(':').first() }
    }

    @Memoized
    List<String> getConfigMaps() {
        return getProcessOutput("kubectl get configmaps | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    List<String> getSecrets() {
        return getProcessOutput("kubectl get secrets --field-selector type=Opaque | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    String getProcessOutput(String command) {
        Process p = ['bash', '-c', command].execute()
        p.waitFor()
        String text = p.text

//        println "****"
//        println "Command: ${command}. Output:\n"
//        println text
//        println "****"
        return text
    }
}