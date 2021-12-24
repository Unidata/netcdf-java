subprojects {
    project.version = "8.0.0-SNAPSHOT"
    project.group = "edu.ucar"
}

tasks.wrapper {
    gradleVersion = "7.3.3"
    distributionSha256Sum = "c9490e938b221daf0094982288e4038deed954a3f12fb54cbf270ddf4e37d879"
    distributionType = Wrapper.DistributionType.ALL
}
