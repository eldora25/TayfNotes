import 'package:flutter/material.dart';

class ChecklistWidget extends StatefulWidget {
  final List<String> items;
  final ValueChanged<List<String>> onChanged;

  const ChecklistWidget({super.key, required this.items, required this.onChanged});

  @override
  State<ChecklistWidget> createState() => _ChecklistWidgetState();
}

class _ChecklistWidgetState extends State<ChecklistWidget> {
  late List<Map<String, dynamic>> _checklist;
  final TextEditingController _controller = TextEditingController();

  @override
  void initState() {
    super.initState();
    _checklist = widget.items.map((item) {
      final isCompleted = item.startsWith('1_');
      final text = item.length > 2 ? item.substring(2) : item;
      return {'completed': isCompleted, 'text': text};
    }).toList();
  }

  void _updateParent() {
    final formatted = _checklist.map((e) => '${e['completed'] ? '1' : '0'}_${e['text']}').toList();
    widget.onChanged(formatted);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _controller,
                decoration: const InputDecoration(hintText: 'Yeni görev ekle...'),
                onSubmitted: (value) {
                  if (value.trim().isNotEmpty) {
                    setState(() {
                      _checklist.add({'completed': false, 'text': value.trim()});
                      _controller.clear();
                      _updateParent();
                    });
                  }
                },
              ),
            ),
            IconButton(
              icon: const Icon(Icons.add_task),
              onPressed: () {
                if (_controller.text.trim().isNotEmpty) {
                  setState(() {
                    _checklist.add({'completed': false, 'text': _controller.text.trim()});
                    _controller.clear();
                    _updateParent();
                  });
                }
              },
            ),
          ],
        ),
        const SizedBox(height: 10),
        Expanded(
          child: ListView.builder(
            itemCount: _checklist.length,
            itemBuilder: (context, index) {
              final item = _checklist[index];
              return CheckboxListTile(
                title: Text(
                  item['text'],
                  style: TextStyle(
                    decoration: item['completed'] ? TextDecoration.lineThrough : null,
                    color: item['completed'] ? Colors.grey : null,
                  ),
                ),
                value: item['completed'],
                onChanged: (val) {
                  setState(() {
                    item['completed'] = val ?? false;
                    _updateParent();
                  });
                },
                secondary: IconButton(
                  icon: const Icon(Icons.delete_outline, size: 20, color: Colors.red),
                  onPressed: () {
                    setState(() {
                      _checklist.removeAt(index);
                      _updateParent();
                    });
                  },
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}
