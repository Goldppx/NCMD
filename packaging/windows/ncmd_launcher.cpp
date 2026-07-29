#include <windows.h>
#include <shellapi.h>

#include <filesystem>
#include <fstream>
#include <optional>
#include <regex>
#include <string>
#include <vector>

namespace {

constexpr int kMinimumJavaVersion = 21;
constexpr wchar_t kRuntimeUrl[] = L"https://github.com/JetBrains/JetBrainsRuntime/releases";
constexpr wchar_t kMainClass[] = L"com.gem.neteasecloudmd.desktop.MainKt";
constexpr wchar_t kAppVersion[] = L"@NCMD_VERSION@";

std::wstring quoteArgument(const std::wstring& value) {
    std::wstring quoted = L"\"";
    size_t backslashCount = 0;
    for (const wchar_t character : value) {
        if (character == L'\\') {
            ++backslashCount;
        } else if (character == L'\"') {
            quoted.append(backslashCount * 2 + 1, L'\\');
            quoted.push_back(character);
            backslashCount = 0;
        } else {
            quoted.append(backslashCount, L'\\');
            quoted.push_back(character);
            backslashCount = 0;
        }
    }
    quoted.append(backslashCount * 2, L'\\');
    quoted += L"\"";
    return quoted;
}

std::optional<std::wstring> readEnvironmentVariable(const wchar_t* name) {
    const DWORD requiredLength = GetEnvironmentVariableW(name, nullptr, 0);
    if (requiredLength == 0) {
        return std::nullopt;
    }

    std::wstring value(requiredLength, L'\0');
    GetEnvironmentVariableW(name, value.data(), requiredLength);
    value.resize(requiredLength - 1);
    return value;
}

std::optional<std::wstring> javaFromHome(const std::filesystem::path& javaHome) {
    const auto javaw = javaHome / L"bin" / L"javaw.exe";
    if (std::filesystem::is_regular_file(javaw)) {
        return javaw.wstring();
    }
    return std::nullopt;
}

std::optional<std::wstring> readRegistryString(HKEY root, const std::wstring& keyPath, const wchar_t* valueName) {
    HKEY key = nullptr;
    if (RegOpenKeyExW(root, keyPath.c_str(), 0, KEY_READ | KEY_WOW64_64KEY, &key) != ERROR_SUCCESS) {
        return std::nullopt;
    }

    DWORD type = 0;
    DWORD byteCount = 0;
    const auto sizeResult = RegQueryValueExW(key, valueName, nullptr, &type, nullptr, &byteCount);
    if (sizeResult != ERROR_SUCCESS || (type != REG_SZ && type != REG_EXPAND_SZ)) {
        RegCloseKey(key);
        return std::nullopt;
    }

    std::wstring value(byteCount / sizeof(wchar_t), L'\0');
    const auto readResult = RegQueryValueExW(
        key,
        valueName,
        nullptr,
        &type,
        reinterpret_cast<BYTE*>(value.data()),
        &byteCount
    );
    RegCloseKey(key);
    if (readResult != ERROR_SUCCESS) {
        return std::nullopt;
    }

    value.resize(value.find(L'\0'));
    return value;
}

std::optional<std::wstring> javaFromRegistry(const wchar_t* rootPath) {
    const auto currentVersion = readRegistryString(HKEY_LOCAL_MACHINE, rootPath, L"CurrentVersion");
    if (!currentVersion) {
        return std::nullopt;
    }

    const std::wstring versionKey = std::wstring(rootPath) + L"\\" + *currentVersion;
    const auto javaHome = readRegistryString(HKEY_LOCAL_MACHINE, versionKey, L"JavaHome");
    return javaHome ? javaFromHome(*javaHome) : std::nullopt;
}

std::optional<int> javaVersion(const std::filesystem::path& javawPath) {
    const auto releaseFile = javawPath.parent_path().parent_path() / L"release";
    std::wifstream input(releaseFile);
    if (!input) {
        return std::nullopt;
    }

    const std::wregex versionPattern(LR"(JAVA_VERSION="([0-9]+))");
    std::wstring line;
    while (std::getline(input, line)) {
        std::wsmatch match;
        if (std::regex_search(line, match, versionPattern)) {
            return std::stoi(match[1].str());
        }
    }
    return std::nullopt;
}

std::optional<std::wstring> usableJava(const std::optional<std::wstring>& candidate) {
    if (!candidate) {
        return std::nullopt;
    }

    const auto version = javaVersion(*candidate);
    if (version && *version >= kMinimumJavaVersion) {
        return candidate;
    }
    return std::nullopt;
}

std::optional<std::wstring> findJava() {
    for (const wchar_t* variable : {L"NCMD_JAVA_HOME", L"JAVA_HOME"}) {
        if (const auto home = readEnvironmentVariable(variable)) {
            if (const auto java = usableJava(javaFromHome(*home))) {
                return java;
            }
        }
    }

    for (const wchar_t* key : {L"SOFTWARE\\JavaSoft\\JRE", L"SOFTWARE\\JavaSoft\\JDK"}) {
        if (const auto java = usableJava(javaFromRegistry(key))) {
            return java;
        }
    }

    if (const auto path = readEnvironmentVariable(L"PATH")) {
        size_t start = 0;
        while (start <= path->size()) {
            const size_t end = path->find(L';', start);
            const auto directory = path->substr(start, end - start);
            if (!directory.empty()) {
                const auto candidate = (std::filesystem::path(directory) / L"javaw.exe").wstring();
                if (const auto java = usableJava(std::optional<std::wstring>{candidate})) {
                    return java;
                }
            }
            if (end == std::wstring::npos) {
                break;
            }
            start = end + 1;
        }
    }

    return std::nullopt;
}

std::filesystem::path executableDirectory() {
    std::vector<wchar_t> path(MAX_PATH);
    DWORD length = 0;
    do {
        length = GetModuleFileNameW(nullptr, path.data(), static_cast<DWORD>(path.size()));
        if (length < path.size() - 1) {
            path.resize(length);
            return std::filesystem::path(std::wstring(path.data(), path.size())).parent_path();
        }
        path.resize(path.size() * 2);
    } while (path.size() <= 32768);
    return {};
}

void showMissingRuntimeDialog() {
    const int answer = MessageBoxW(
        nullptr,
        L"NCMD needs a system-installed Java 21 runtime.\n\n"
        L"Install JetBrains Runtime 21 or newer, then start NCMD again. "
        L"Choose OK to open the official download page.",
        L"NCMD - Java runtime required",
        MB_OKCANCEL | MB_ICONINFORMATION
    );
    if (answer == IDOK) {
        ShellExecuteW(nullptr, L"open", kRuntimeUrl, nullptr, nullptr, SW_SHOWNORMAL);
    }
}

}  // namespace

int WINAPI wWinMain(HINSTANCE, HINSTANCE, PWSTR, int) {
    SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);

    const auto appRoot = executableDirectory();
    const auto appDirectory = appRoot / L"lib" / L"app";
    if (appRoot.empty() || !std::filesystem::is_directory(appDirectory)) {
        MessageBoxW(nullptr, L"NCMD installation is incomplete. Reinstall NCMD.", L"NCMD", MB_OK | MB_ICONERROR);
        return 1;
    }

    const auto java = findJava();
    if (!java) {
        showMissingRuntimeDialog();
        return 1;
    }

    std::wstring command = quoteArgument(*java);
    command += L" -Djpackage.app-version=" + std::wstring(kAppVersion);
    command += L" " + quoteArgument(L"-Dcompose.application.resources.dir=" + (appDirectory / L"resources").wstring());
    command += L" -Dcompose.application.configure.swing.globals=true";
    command += L" " + quoteArgument(L"-Dskiko.library.path=" + appDirectory.wstring());
    command += L" -cp " + quoteArgument((appDirectory / L"*").wstring());
    command += L" " + std::wstring(kMainClass);

    STARTUPINFOW startupInfo{};
    startupInfo.cb = sizeof(startupInfo);
    PROCESS_INFORMATION processInfo{};
    if (!CreateProcessW(nullptr, command.data(), nullptr, nullptr, FALSE, 0, nullptr, appRoot.c_str(), &startupInfo, &processInfo)) {
        MessageBoxW(nullptr, L"NCMD could not start Java. Check the Java installation and try again.", L"NCMD", MB_OK | MB_ICONERROR);
        return 1;
    }

    CloseHandle(processInfo.hThread);
    CloseHandle(processInfo.hProcess);
    return 0;
}
