import 'package:image_picker/image_picker.dart';

extension XFileExtension on XFile {
  String get extension => name.split('.').last.toLowerCase();
  int get size => 0;
}
