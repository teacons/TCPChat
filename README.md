# Build:

Build artifacts in `build` folder

## Windows
`gradlew.bat build`

## Linux
`./gradlew build`

# Protocol:

## Connecting to server:

- Client send command `CONNECT` to server
- Server validate username and time
- If ok then server send to client command `ACCEPT`
- Else server send command `DISCONNECT` with reason

## Packet format:

| command |  time  | username_length | data_length |        username        |        data        |
|:-------:|:------:|:---------------:|:-----------:|:----------------------:|:------------------:|
| 1 byte  | 4 byte |     1 byte      |   4 byte    | `username_length` byte | `data_length` byte |


## command (1 byte):

- `0` - MESSAGE,
- `1` - CONNECT,
- `2` - ACCEPT,
- `3` - DISCONNECT,
- `4` - FILE.

##time (4 byte):
- Unix time in bytes field

##username_length (1 byte):
- size of username field

##data_length (3 byte):
- size of data field

##username (`username_length` bytes):
- username field
- max 255 bytes

##data (`data_length` bytes):
- data field,
- max 16777215 bytes

