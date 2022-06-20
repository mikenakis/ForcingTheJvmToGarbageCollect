#include <stdlib.h>
#include <stdio.h>
#include <jvmti.h>

typedef struct {
 jvmtiEnv *jvmti;
} GlobalAgentData;

static GlobalAgentData *gdata;

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
  printf("load garbager agent\n");
  jvmtiEnv *jvmti = NULL;

  // put a jvmtiEnv instance at jvmti.
  jint result = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
  if (result != JNI_OK) {
    printf("ERROR: Unable to access JVMTI!\n");
  }

  // store jvmti in a global data
  gdata = (GlobalAgentData*) malloc(sizeof(GlobalAgentData));
  gdata->jvmti = jvmti;
  return JNI_OK;
}


extern "C"
JNIEXPORT int JNICALL Java_mikenakis_Garbager_forceGarbageCollection(JNIEnv *env, jclass thisClass) 
{
  return gdata->jvmti->ForceGarbageCollection();
}
