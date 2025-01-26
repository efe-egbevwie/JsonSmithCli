# JsonSmith

A CLI application to generate classes for different languages from json files 

## Usage
``` bash
  JsonSmith --file='<file-path>' --language=<language> --output='<output-file=path>'"
```
or

```bash
      JsonSmith -f'<file-path>' -l=<language> -o='<output-file=path>'"
```
### Example
```Bash
    .\JsonSmith -f='jsonString.json' -l=java -o='C:files\JsonSmithCli\'
```

## Building
To build, run the gradle task
```Bash
    ./gradlew nativeBinaries
```
Then run the app from the `build/bin/native/debugExecutable/`
```Bash
    ./JsonSmith -f'<file-path>' -l=<language> -o='<output-file=path>'"
```