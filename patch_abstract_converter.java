<<<<<<< SEARCH
                            String fileName = targetFile.getFileName().toString();
                            int dotIndex = fileName.lastIndexOf('.');
                            if (dotIndex > 0) {
                                fileName = fileName.substring(0, dotIndex) + getTargetExtension();
                            } else {
                                fileName = fileName + getTargetExtension();
                            }
                            targetFile = targetFile.resolveSibling(fileName);
=======
                            String ext = getTargetExtension();
                            if (ext != null && !ext.isEmpty()) {
                                String fileName = targetFile.getFileName().toString();
                                int dotIndex = fileName.lastIndexOf('.');
                                if (dotIndex > 0) {
                                    fileName = fileName.substring(0, dotIndex) + ext;
                                } else {
                                    fileName = fileName + ext;
                                }
                                targetFile = targetFile.resolveSibling(fileName);
                            }
>>>>>>> REPLACE
<<<<<<< SEARCH
                String fileName = source.getFileName().toString();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileName = fileName.substring(0, dotIndex) + getTargetExtension();
                } else {
                    fileName = fileName + getTargetExtension();
                }
                targetFile = target.resolve(fileName);
=======
                String ext = getTargetExtension();
                String fileName = source.getFileName().toString();
                if (ext != null && !ext.isEmpty()) {
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        fileName = fileName.substring(0, dotIndex) + ext;
                    } else {
                        fileName = fileName + ext;
                    }
                }
                targetFile = target.resolve(fileName);
>>>>>>> REPLACE
