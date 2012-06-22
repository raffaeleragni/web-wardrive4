package ki.wardrive4.web.utils;

/**
 * Equivalent of StringBuilder for JSON scope.
 * NON-threadsafe!
 *
 * @author Raffaele Ragni
 */
public class JSONBuilder
{
	private StringBuilder sb;

	private boolean needComma;

	public JSONBuilder()
	{
		sb = new StringBuilder();
	}

	public JSONBuilder(int capacity)
	{
		sb = new StringBuilder(capacity);
	}

	public void beginArray()
	{
		if (needComma)
		{
			sb.append(',');
		}

		sb.append('[');
		needComma = false;
	}

    public void beginArray(String name)
	{
		if (needComma)
		{
			sb.append(',');
		}

        insert_with_quote(name);
        sb.append(':');
		sb.append('[');
		needComma = false;
	}

	public void endArray()
	{
		sb.append(']');
		needComma = true;
	}

	public void beginObject()
	{
		if (needComma)
		{
			sb.append(',');
		}

		sb.append('{');
		needComma = false;
	}

	public void endObject()
	{
		sb.append('}');
		needComma = true;
	}

    public void value(Object value)
    {
        if (needComma)
		{
			sb.append(',');
		}

        if (value instanceof String)
		{
			insert_with_quote((String) value);
		}
		else
		{
			sb.append(value);
		}

        needComma = true;
    }

	public void property(String name, Object value)
	{
		if (name == null)
		{
			return;
		}

		if (needComma)
		{
			sb.append(',');
		}

		insert_with_quote(name);

		sb.append(':');

		if (value instanceof String)
		{
			insert_with_quote((String) value);
		}
		else
		{
			sb.append(value);
		}

		needComma = true;
	}

	public void append(Object value)
	{
		if (needComma)
		{
			sb.append(',');
		}

		if (value instanceof String)
		{
			insert_with_quote((String) value);
		}
		else
		{
			sb.append(value);
		}

		needComma = true;
	}

	@Override
	public String toString()
	{
		return sb.toString();
	}

	private void insert_with_quote(String s)
	{
		int len = s != null ? s.length() : 0;
		
		if (len == 0)
		{
			sb.append('"');
			sb.append('"');
			return;
		}

		char b;
		char c = 0;
		int i;

		sb.append('"');
		for (i = 0; i < len; i += 1)
		{
			b = c;
			c = s.charAt(i);
			switch (c)
			{
				case '\\':
				case '"':
					sb.append('\\');
					sb.append(c);
					break;
				case '/':
					if (b == '<')
					{
						sb.append('\\');
					}
					sb.append(c);
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\r':
					sb.append("\\r");
					break;
				default:
					if (c < ' ' || c >= '\u0080' && c < '\u00a0' || c >= '\u2000' && c < '\u2100')
					{
						String t = "000" + Integer.toHexString(c);
						sb.append("\\u").append(t.substring(t.length() - 4));
					}
					else
					{
						sb.append(c);
					}
			}
		}
		sb.append('"');
	}
}