function renderInline(text) {
  const parts = text.split(/(\*\*[^*]+\*\*|`[^`]+`)/g);
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    if (part.startsWith('`') && part.endsWith('`')) {
      return (
        <code key={index} className="rounded bg-slate-100 px-1.5 py-0.5 text-xs text-indigo-700">
          {part.slice(1, -1)}
        </code>
      );
    }
    return part;
  });
}

export default function MarkdownText({ content }) {
  const lines = content.split('\n');
  const blocks = [];
  let listType = null;
  let listItems = [];

  const flushList = () => {
    if (listType) {
      const List = listType === 'ul' ? 'ul' : 'ol';
      blocks.push(
        <List key={`list-${blocks.length}`} className="my-2 space-y-1 pl-5">
          {listItems.map((item, index) => (
            <li key={index} className="list-disc text-sm leading-6 text-slate-600 marker:text-slate-400">
              {item}
            </li>
          ))}
        </List>
      );
      listType = null;
      listItems = [];
    }
  };

  lines.forEach((line, index) => {
    if (line.startsWith('### ')) {
      flushList();
      blocks.push(
        <h4 key={`h4-${index}`} className="mt-3 text-base font-semibold text-slate-800">
          {renderInline(line.slice(4))}
        </h4>
      );
      return;
    }
    if (line.startsWith('## ')) {
      flushList();
      blocks.push(
        <h3 key={`h3-${index}`} className="mt-4 text-lg font-semibold text-slate-900">
          {renderInline(line.slice(3))}
        </h3>
      );
      return;
    }
    if (line.startsWith('# ')) {
      flushList();
      blocks.push(
        <h2 key={`h2-${index}`} className="mt-4 text-xl font-semibold text-slate-900">
          {renderInline(line.slice(2))}
        </h2>
      );
      return;
    }

    if (/^[-*]\s+/.test(line)) {
      if (listType !== 'ul') {
        flushList();
        listType = 'ul';
      }
      listItems.push(renderInline(line.replace(/^[-*]\s+/, '')));
      return;
    }

    if (/^\d+\.\s+/.test(line)) {
      if (listType !== 'ol') {
        flushList();
        listType = 'ol';
      }
      listItems.push(renderInline(line.replace(/^\d+\.\s+/, '')));
      return;
    }

    if (line.trim() === '') {
      flushList();
      return;
    }

    flushList();
    blocks.push(
      <p key={`p-${index}`} className="text-sm leading-6 text-slate-600">
        {renderInline(line)}
      </p>
    );
  });

  flushList();

  return <div>{blocks}</div>;
}
