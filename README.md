# mongodbdump-java-wrapper
A simple java wrapper for mongodb backup commands : mongodump & mongorestore

## Goal
Goal is to provide to any java application, a simple and light way to backup a Mongo database:
- first locally through zip file on the local storage
- (planned) second remotely using custom plugin (example: box.com plugin to store backup zip file on the cloud : a remote secure box directory)

## Integration tests (MongodumpServiceITest)
- backup full database locally
  -  GIVEN: database name
  -  WHEN: BACKUP action
  -  THEN: fresh backup zip file created from the database
- restore locally
  -  GIVEN: database name, backup file name
  -  WHEN: RESTORE action
  -  THEN: fresh database restore done from backup zip file

### Contributions
Contributions are welcome through feature branch and pull request. 

If you encounter issue, please provide details on a new [ticket](https://github.com/boly38/mongodbdump-java-wrapper/issues).

### TODO list
 - add cloud save/restore feature (ex. a [box.com](https://www.box.com/) directory)
 - add the way to auto-detect mongo executables on an openshift context (at runtime on an openshift gear)
