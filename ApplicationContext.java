import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class ApplicationContext {
	// 存放dom
	private Document dom;
	private Element root;
	// 从配置中生成的bean放入map中
	private HashMap<String, Object> beans = new HashMap<String, Object>();

	public void ClassPathXmlApplicationContext(String xmlUrl){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder db;
		try {
			db = factory.newDocumentBuilder();
			dom = db.parse(xmlUrl);
			dom.normalize();
			root = dom.getDocumentElement();
			System.out.println("初始化开始...");
			initBeans();
			System.out.println("初始化结束...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	// 初始化配置文件中所有非懒加载的bean
	private void initBeans() {
		for (Node node = root.getFirstChild(); node != null; node = node
				.getNextSibling()) {
			// 延迟加载Bean
			if (node.getNodeName().equals("bean")
					&& ((Element) node).hasAttribute("lazy-init")
					&& ((Element) node).getAttribute("lazy-init")
							.equals("true"))
				continue;
			if (node.getNodeType() == Node.ELEMENT_NODE)
				// 创建后，存入map中
				beans.put(((Element) node).getAttribute("id"), initBean(node));
		}
	}

	private Object initBean(Node node) {
		// 初始化的类型
		Class c = null;
		// 需要初始化的bean
		Object object = null;
		// 生成对象
		try {
			c = Class.forName(((Element) node).getAttribute("class"));
			object = c.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// 如果此bean为基本类型bean
		if (isPrimitive(((Element) node).getAttribute("class"))
				|| ((Element) node).hasAttribute("value")) {
			object = getInstanceForName(((Element) node).getAttribute("class"),
					((Element) node).getAttribute("value"));
			return object;
		}


		//boolean constructorInit = false;//是否为构造函数注入
		ArrayList<Class> parameterTypes = new ArrayList<Class>();
		ArrayList<Object> parameters = new ArrayList<Object>();
		// 为bean配置属性
		for (Node property = node.getFirstChild(); property != null; property = property
				.getNextSibling()) {
			if (property.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			if (property.getNodeName().equals("constructor-arg")) {
				// bean为构造函数注入
				//constructorInit = true;
				if (((Element)property).hasAttribute("value")) {
					//该参数为基本类型参数
					getInstanceForName(((Element)property).getAttribute("type"), ((Element)property).getAttribute("value"));
					Class temp = nameToPrimitiveClass(((Element)property).getAttribute("type"));
					parameterTypes.add(temp);
					parameters.add(getInstanceForName(((Element)property).getAttribute("type"), ((Element)property).getAttribute("value")));
				} else if (((Element)property).hasAttribute("ref")){
					String refId = ((Element) property).getAttribute("ref");
					// 该参数为外部bean引用属性
					if (beans.containsKey(refId)) {
						// 在beans的map中发现已初始化过该bean
						parameterTypes.add(beans.get(refId).getClass());
						parameters.add(beans.get(refId));
					} else {
						// 未初始化过该bean
						Node e = dom.getElementById(refId);
						if (e.getNodeName().equals("bean")) {
							// 递归调用，初始化被引用的bean
							Class temp = null;
							try {
								temp = Class.forName(((Element)e).getAttribute("class"));
							} catch (ClassNotFoundException e1) {
								e1.printStackTrace();
							}
							parameterTypes.add(temp);
							parameters.add(initBean(e));
						}
					}
				}
			} 

				
			}
		

	
		
			Class[] aClasses = {};
			Class[] classes = (Class[]) parameterTypes.toArray(aClasses);
			Object[] prams = (Object[])parameters.toArray();
			try {
				object = c.getConstructor(classes).newInstance(prams);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		

		return object;
	}

	// 获取hashMap中相应的bean
	public Object getBean(String id) {
		Object bean = beans.get(id);
		// 未初始化的bean，有可能为懒加载，也有可能没有此bean
		if (bean == null) {
			Element element = dom.getElementById(id);
			if (element.getTagName().equals("bean")
					&& element.hasAttribute("lazy-init")
					&& element.getAttribute("lazy-init").equals("true")) {
				// 确实为懒加载
				// 创建后，存入map中
				System.out.println("懒加载：" + id);
				bean = initBean(element);
				beans.put(element.getAttribute("id"), bean);
			} else {
				// 无此Bean的懒加载声明
				System.out.println("找不到此Bean");
			}
		}
		return bean;
	}

	// 检验是否为基本值类型或基本对象
	@SuppressWarnings("rawtypes")
	private boolean isPrimitive(String className) {
		Class clazz = null;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		String name = clazz.getName();
		if (clazz.isPrimitive() || name.equals("java.lang.String")
				|| name.equals("java.lang.Integer")
				|| name.equals("java.lang.Float")
				|| name.equals("java.lang.Double")
				|| name.equals("java.lang.Character")
				|| name.equals("java.lang.Integer")
				|| name.equals("java.lang.Boolean")
				|| name.equals("java.lang.Short")) {
			return true;
		} else {
			return false;
		}
	}

	// 通过字符串反射类型，增加了对基本类型的包装
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getInstanceForName(String name, String value) {
		Class clazz = nameToClass(name);

		Object object = null;
		try {
			object = clazz.getConstructor(String.class).newInstance(value);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return object;
	}

	@SuppressWarnings("rawtypes")
	private Class nameToPrimitiveClass(String name){
		Class clazz = null;
		if (name.equals("int")) {
			clazz = int.class;
		} else if (name.equals("char")) {
			clazz = char.class;
		} else if (name.equals("boolean")) {
			clazz = boolean.class;
		} else if (name.equals("short")) {
			clazz = short.class;
		} else if (name.equals("long")) {
			clazz = long.class;
		} else if (name.equals("float")) {
			clazz = float.class;
		} else if (name.equals("double")) {
			clazz = double.class;
		} else if (name.equals("byte")) {
			clazz = byte.class;
		} else {
			try {
				clazz = Class.forName(name);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return clazz;
	}

	@SuppressWarnings("rawtypes")
	private Class nameToClass(String name){
		Class clazz = null;
		if (name.equals("int")) {
			clazz = Integer.class;
		} else if (name.equals("char")) {
			clazz = Character.class;
		} else if (name.equals("boolean")) {
			clazz = Boolean.class;
		} else if (name.equals("short")) {
			clazz = Short.class;
		} else if (name.equals("long")) {
			clazz = Long.class;
		} else if (name.equals("float")) {
			clazz = Float.class;
		} else if (name.equals("double")) {
			clazz = Double.class;
		} else if (name.equals("byte")) {
			clazz = Byte.class;
		} else {
			try {
				clazz = Class.forName(name);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return clazz;
	}

}
