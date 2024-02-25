# PSHE user client

## File patching - locally

### Summary

File mode can be used for creating and applying patches for folders and individual files. You need two instances of project ("old" and "new") to create patch files.

Structure of folder with patches resembles structure of project files with additional "_patch" part in filename.

If file during update is deleted no patch file will be generated.

Two patch subfolders is generated in specified patch folder:

- "forward" - used to update from "old" version to "new".
- "backward" - used to update from "new" version to "old".

### "Patching" tab

Used to apply patch files to project.

"Remember" checkbox selects whether the choosen paths will be saved the next time program is run.

If "Replase old files" is selected - pathed version of files will replace the old ones. Otherwise patched files will be stored in project parent folder in subfolder "patched_tmp".

### "Admin" tab

Used to create patch files from two instances of same project. You need to login for this (TODO).

"Remember" checkbox selects whether the choosen paths will be saved the next time program is run.
