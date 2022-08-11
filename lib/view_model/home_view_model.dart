import 'package:file_picker/file_picker.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mell_pdf/helper/helpers.dart';
import 'package:mell_pdf/model/file_read.dart';
import 'package:mell_pdf/model/file_manager.dart';

class HomeViewModel {
  final FileManager _mfl = AppSession.singleton.mfl;

  Future<void> loadFilesFromStorage() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles(
      allowMultiple: true,
      type: FileType.custom,
      allowedExtensions: ['jpg', 'jpeg', 'pdf', 'png'],
    );
    _mfl.addMultipleFiles(result?.files ?? [], _mfl.fileHelper.localPath);
  }

  Future<void> loadImagesFromStorage() async {
    final ImagePicker picker = ImagePicker();
    // Pick an image
    final List<XFile>? images = await picker.pickMultiImage();
    if (images != null) {
      List<FileRead> files =
          await IsolateHelper.createAddMultiplesImagesIsolate(images);
      _mfl.addFilesInMemory(files);
    }
  }

  FileManager getMergeableFilesList() => _mfl;

  bool thereAreFilesLoaded() => _mfl.hasAnyFile();

  FileRead removeFileFromDisk(int index) => _mfl.removeFileFromDisk(index);

  void removeFileFromDiskByFile(FileRead file) =>
      _mfl.removeFileFromDiskByFile(file);

  FileRead removeFileFromList(int index) => _mfl.removeFileFromList(index);

  void insertFileIntoList(int index, FileRead file) =>
      _mfl.insertFile(index, file);

  void rotateImageInMemoryAndFile(FileRead file) {
    _mfl.rotateImageInMemoryAndFile(file);
  }

  Future<void> resizeImageInMemoryAndFile(
      FileRead file, int width, int height) async {
    final resizedFile =
        await IsolateHelper.createResizeIsolate(file, width, height);
    file.setImage(resizedFile.getImage()!);
    await ImageHelper.updateCache(file);
  }

  Future<void> renameFile(FileRead file, String newName) async {
    await _mfl.renameFile(file, newName);
  }

  Future<FileRead?> scanDocument() async {
    return await _mfl.scanDocument();
  }

  Future<FileRead> generatePreviewPdfDocument() async {
    const fileName = 'Preview Document.pdf';
    final lp = AppSession.singleton.fileHelper.localPath;
    final pathFinal = '$lp$fileName';
    AppSession.singleton.fileHelper.removeIfExist(pathFinal);
    return await _mfl.generatePreviewPdfDocument(pathFinal, fileName);
  }
}
