@file:JvmName("PythonParser")

package core.utils
import kotlin.concurrent.thread

val code: String
    get(){
        return """
import json
import math
import itertools
import threading
import os
import sys
import inspect
import datetime
import collections
code = ""
def fetch_funcs(module, module_name):
    data = []
    for name, obj in inspect.getmembers(module):
        if name.startswith("_"): continue
        if inspect.isfunction(obj) or inspect.isbuiltin(obj) or inspect.isclass(obj):
            doc = inspect.getdoc(obj)
            if doc:
                data.append([name, doc])
            else:
                data.append([name, "No description"])
    return {
        "name": f"[module] {module_name}",
        "package": module_name,
        "funcs": data
    }


def base_funcs():
    data = []
    for name in dir(__builtins__):
        if name.startswith("_"): continue
        obj = getattr(__builtins__, name)
        if inspect.isfunction(obj) or inspect.isbuiltin(obj) or inspect.isclass(obj):
            doc = inspect.getdoc(obj)
            if doc:
                data.append([name, doc])
            else:
                data.append([name, "No description"])
    return {
        "name": "[basefunc]",
        "package": "",
        "funcs": data
    }


def main():
    partitions = [base_funcs()]
    modules = {
        "math": math,
        "itertools": itertools,
        "threading": threading,
        "os": os,
        "sys": sys,
        "datetime": datetime,
        "collections": collections
    }
    for name, module in modules.items():
        partitions.append(fetch_funcs(module, name))
    print(json.dumps(
            {"partitions": partitions},
            ensure_ascii=False,
            indent=4
        ))
main()
sys.exit(0)
""".replace("\"", "'")
    }

val replaceData = listOf(
    "[basefunc]" to "Основные функции",
    "[module]" to "Модуль"
)

fun getFunctionsJson(): String {
    val proc = ProcessBuilder("python", "-c", code)
        .start()
    val out = StringBuilder()
    thread {
        proc.inputStream.bufferedReader(Charsets.UTF_8).forEachLine {
            out.append(it).append("\n")
        }
    }
    proc.waitFor()
    var res =  out.toString()
    replaceData.forEach {
        res = res.replace(it.first, it.second)
    }

    return res
}
