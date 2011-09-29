package jlm.core.model.lesson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import jlm.core.model.Game;
import jlm.core.model.LogWriter;
import jlm.core.model.ProgrammingLanguage;
import jlm.universe.Entity;
import jlm.universe.World;


public abstract class Exercise  {
	public boolean debug = false; /* whether to debug this particular exo */

	protected boolean done = false; /** indicate whether this Exercise was successfully done or not */

	public String name = "<no name>"; 
	protected String mission = "";  /** The text to display to present the lesson */
	static protected Map<ProgrammingLanguage,String> css; /** The CSS to use for a given language */
	public String hint = null;
	
	protected Map<String, String> tips = new HashMap<String, String>();
	
	protected Map<ProgrammingLanguage, List<SourceFile>> sourceFiles; /** All the editable source files */

	
	public Map<String, Class<Object>> compiledClasses = new TreeMap<String, Class<Object>>(); /* list of entity classes defined in the lesson */
	public Map<ProgrammingLanguage, Map<String, String>> scriptSources = new TreeMap<ProgrammingLanguage, Map<String, String>>(); /* (Lang x scriptName |-> script source) list of scripts that are to be executed by entities when not running Java */

	/* to make sure that the subsequent version of the same class have different names, in order to bypass the cache of the class loader */
	private static final String packageNamePrefix = "jlm.runtime";
	private int packageNameSuffix = 0;

	protected World [] currentWorld; /* the one displayed */
	protected World [] initialWorld; /* the one used to reset the previous on each run */
	protected World [] answerWorld;  /* the one current should look like to pass the test */

	protected Map<String, String> runtimePatterns;


	private Lesson lesson; 

	public String getName() {
		return this.name;
	}

	public void successfullyPassed() {
		this.done = true;
	}
	
	public void failed() {
		this.done = false;
	}

	public boolean isSuccessfullyPassed() {
		return this.done;
	}

	public Lesson getLesson() {
		return this.lesson;
	}
	
	protected static String getCSS(ProgrammingLanguage lang) {
		if (css==null) {
			 css = new HashMap<ProgrammingLanguage, String>();
			 for (ProgrammingLanguage l : Game.programmingLanguages) {
				 String theCSS = 
						"  <style type=\"text/css\">\n"+
				        "    body { font-family: tahoma, \"Times New Roman\", serif; font-size:10px; margin:10px; }\n"+
				        "    code { background:#EEEEEE; }\n"+
				        "    pre { background: #EEEEEE;\n"+
				        "          margin: 5px;\n"+
				        "          padding: 6px;\n"+
				        "          border: 1px inset;\n"+
				        "          width: 640px;\n"+
				        "          overflow: auto;\n"+
				        "          text-align: left;\n"+
				        "          font-family: \"Courrier New\", \"Courrier\", monospace; }\n"+
				        "   .comment { background:#EEEEEE;\n"+
				        "              font-family: \"Times New Roman\", serif;\n"+
				        "              color:#00AA00;\n"+
				        "              font-style: italic; }\n";
				 for (ProgrammingLanguage l2 : Game.programmingLanguages) {
					 if (!l.equals(l2)) {
						 theCSS += "."+l2.getLang()+" {display: none; color:#0000FF;}\n";
						 theCSS += "."+l2.getLang().toLowerCase()+" {display: none; color:#0000FF;}\n";
					 } else {
						 /* DEBUG ONLY, to see the specific elements 
						 theCSS += "."+l2.getLang()+" {visibility: visible; color:#00AA00;}\n";
						 theCSS += "."+l2.getLang().toLowerCase()+" {visibility: visible; color:#00AA00;}\n";
						 */
					 }
				 }
				 theCSS +=  "  </style>\n";
				 css.put(l, theCSS);
			 }
		}
		if (css.get(lang)==null)
			throw new RuntimeException("Damn, no CSS for lang "+lang+". I'm puzzled");
		return css.get(lang);
	}
	public String getMission(ProgrammingLanguage lang) {
		String res = "<html><head>"+getCSS(lang)+"</head><body>"+this.mission+"</body></html>";
		return res;
	}

	public List<World> getCurrentWorld() {
		return Arrays.asList(currentWorld);
	}
	
	public void worldDuplicate(World[] w) {
		currentWorld = new World[w.length];
		initialWorld = new World[w.length];
		answerWorld  = new World[w.length];
		for (int i=0; i<w.length; i++) {
			currentWorld[i] = w[i].copy();
			initialWorld[i] = w[i].copy();
			answerWorld[i]  = w[i].copy();
		}
	}
	
	public abstract void run(List<Thread> runnerVect);	
	public abstract void runDemo(List<Thread> runnerVect);	
	
	public boolean check() throws Exception {
		for (int i=0; i<currentWorld.length; i++) {
			currentWorld[i].notifyWorldUpdatesListeners();
			if (!currentWorld[i].equals(answerWorld[i]))
				return false;
		}
		return true;
	}
	public void reset() {
		for (int i=0; i<initialWorld.length; i++) 
			currentWorld[i].reset(initialWorld[i]);
	}

	/*
	 * +++++++++++++++++++++++++
	 * Compilation related stuff
	 * +++++++++++++++++++++++++
	 * 
	 */
	//TODO: why do we instantiate a compiler per exercise ? is there any way to re-use the same compiler. I tried to put it as static, but of course strange behaviors happen afterwards
	// Create a compiler of classes (using java 1.6)
	private final InMemoryCompiler compiler = new InMemoryCompiler(
			getClass().getClassLoader(), Arrays.asList(new String[] { "-target", "1.6" }));

	/**
	 * Generate Java source from the user function
	 * @throws JLMCompilerException 
	 */
	public void compileAll(LogWriter out) throws JLMCompilerException {
		compiledClasses = new TreeMap<String, Class<Object>>();

		/* Make sure each run generate a new package to avoid that the loader cache prevent the reloading of the newly generated class */
		packageNameSuffix++;
		runtimePatterns.put("\\$package", "package "+packageName()+";");

		/* Prepare the source files */
		Map<String, CharSequence> sources = new TreeMap<String, CharSequence>();
		for (SourceFile sf: sourceFiles.get(Game.JAVA)) 
			if (sf.isCompilable()) 
				sources.put(className(sf.getName()), sf.getCompilableContent(runtimePatterns)); 

		if (sources.isEmpty())
			return;
		
		/* Do the compile */
		try {
			DiagnosticCollector<JavaFileObject> errs = new DiagnosticCollector<JavaFileObject>();			
			compiledClasses = compiler.compile(sources, errs);

			out.log(errs);
		} catch (JLMCompilerException e) {
			System.err.println("Compilation error:");
			out.log(e.getDiagnostics());
			throw e;
		}
		
		/* Setup the scripts for the other languages */
		for (ProgrammingLanguage lang: getProgLanguages()) {
			if (!lang.equals(Game.JAVA) && !lang.equals(Game.LIGHTBOT)) {
				Map<String, String> scripts = new TreeMap<String, String>();
				
				for (SourceFile sf: sourceFiles.get(lang)) 
					if (sf.isCompilable()) 
						scripts.put(className(sf.getName()), sf.getCompilableContent(null)); 
				
				scriptSources.put(lang,scripts); 
			}
		}
	}
	
	private String packageName(){
		return packageNamePrefix + packageNameSuffix;
	}
	public String className(String name) {
		return packageName() + "." + name;
	}
	/** get the list of source files for a given language, or create it if not existent yet */
	protected List<SourceFile> getSourceFiles(ProgrammingLanguage lang) {
		List<SourceFile> res = sourceFiles.get(lang); 
		if (res == null) {
			res = new ArrayList<SourceFile>();
			sourceFiles.put(lang, res);
		}
		return res;
	}
	/** Add a new unmodifiable source file in child classes, ie alongside to stuff to be written by the student */
	public void newFrozenSource(ProgrammingLanguage lang, String name, String content) {
		SourceFile sf = new SourceFile(name, content);
		sf.setEditable(false);
		getSourceFiles(lang).add(sf);
	}
	public void newFrozenSource(String name, String content) {
		newFrozenSource(Game.JAVA, name, content);
	}
	/** Add a new text file in child classes, ie file that is not going to be compiled  */
	public void newTextFile(ProgrammingLanguage lang, String name, String content) {
		SourceFile sf = new SourceFile(name, content);
		sf.setCompilable(false);
		getSourceFiles(lang).add(sf);
	}
	public void newSourceAliased(String lesson, String exercise, String file) {
		/* FIXME: this should alias for all existing languages */
		newSourceAliased(Game.JAVA, lesson, exercise, file);
	}
	public void newSourceAliased(ProgrammingLanguage lang, String lesson, String exercise, String file) {
		SourceFile sf = new SourceFileAliased(lang, lesson, exercise,file);
		getSourceFiles(lang).add(sf);
	}
	public void newSource(ProgrammingLanguage lang, String name, String initialContent, String template) {
		newSource(lang, name, initialContent, template, "");
	}
	public void newSource(ProgrammingLanguage lang, String name, String initialContent, String template, String patterns) {
		Map<String, String> pat = new TreeMap<String, String>();
		for (String pattern: patterns.split(";")) {
			String[] parts = pattern.split("/");
			if (parts.length != 1 || !parts[0].equals("")) {
				if (parts.length != 3 || !parts[0].equals("s")) 
					throw new RuntimeException("Malformed pattern for file "+name+": '"+ pattern+"' (from '"+patterns+"')");
				pat.put(parts[1], parts[2]);
			}
		}
		getSourceFiles(lang).add(new SourceFileRevertable(name, initialContent, template, pat));
	}
	public SourceFile getSourceFile(ProgrammingLanguage lang, String name) {
		for (SourceFile sf: getSourceFiles(lang)) {
			if (sf.getName().equals(name))
				return sf;
		}
		return null;
	}
	
	protected void mutateEntities(World[] worlds, ArrayList<String> newClasseNames) {
		for (World current:worlds) {
			ArrayList<Entity> newEntities = new ArrayList<Entity>();
			Iterator<Entity> it = current.entities();
			for (String name : newClasseNames) {
				/* Get the next existing entity */
				if (!it.hasNext()) 
					throw new BrokenLessonException("Too much arguments provided to mutateEntities");
				Entity old = it.next();

				if (Game.getProgrammingLanguage().equals(Game.JAVA) || 
						Game.getProgrammingLanguage().equals(Game.LIGHTBOT)) {
					/* Instanciate a new entity of the new type */
					Entity ent;
					try {
						ent = (Entity)compiledClasses.get(className(name)).newInstance();
						//Logger.log("Exercise:mutateEntities to "+className(name), b.toString());
					} catch (InstantiationException e) {
						throw new RuntimeException("Cannot instanciate entity of type "+className(name), e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException("Illegal access while instanciating entity of type "+className(name), e);
					} catch (NullPointerException e) {
						/* this kind of entity was not written by student. try to get it from default class loader, or complain if it also fails */
						try {
							ent = (Entity)compiler.loadClass(name).newInstance(); 
						} catch (Exception e2) {
							throw new RuntimeException("Cannot find an entity of name "+className(name)+" or "+name+". Broken lesson.", e2);
						}
					}

					/* change fields of new entity to copy old one */
					ent.copy(old);
					ent.initDone();

					/* Add new entity to the to be returned entities set */
					newEntities.add(ent);
				} else { /* In scripting, we don't need to actually mutate the entity, just set the script to be interpreted later */
					String script = scriptSources.get(Game.getProgrammingLanguage()).get(className(name));
					if (script == null) 
						throw new RuntimeException("Cannot retrieve the script for "+className(name));
					old.setScript(Game.getProgrammingLanguage(), script);
				}
			}
			if (it.hasNext())
				throw new BrokenLessonException("Not enough arguments provided to mutateEntities");
			if (Game.getProgrammingLanguage().equals(Game.JAVA)) 
				current.setEntities(newEntities);
		}
	}
	protected void mutateEntity(World[] worlds, String newClasseName){
		ArrayList<String> names= new ArrayList<String>();
		for (int i=0; i<currentWorld[0].getEntityCount(); i++)
			names.add(newClasseName);
		mutateEntities(worlds, names);
	}

	protected void mutateEntities(ArrayList<String> newClasseNames){
		mutateEntities(currentWorld, newClasseNames);
	}

	protected void mutateEntity(String newClasseName){		
		mutateEntity(currentWorld, newClasseName);
	}

	
	public Exercise(Lesson lesson) {
		super();
		this.lesson = lesson;
		sourceFiles = new HashMap<ProgrammingLanguage, List<SourceFile>>();
		runtimePatterns = new TreeMap<String, String>();
	}

	@Deprecated
	public String[] getSourceFilesNames(ProgrammingLanguage lang) {
		String[] res = new String[sourceFiles.size()]; // will be too large if not all compilable, but who cares?
		int i = 0;
		for (SourceFile sf: getSourceFiles(lang)) {
			if (sf.isCompilable()) {
				res[i] = sf.getName();
				i++;
			}
		}
		return res;
	}
			
	public List<World> getAnswerWorld() {
		return Arrays.asList(answerWorld);
	}
	
	public List<World> getInitialWorld() {
		return Arrays.asList(this.initialWorld);
	}

	public int publicSourceFileCount(ProgrammingLanguage lang) {
		int res=0;
		for (SourceFile sf : getSourceFiles(lang)) {
			if (sf.isEditable())
				res++;
		}
		return res;
	}
	
	public SourceFile getPublicSourceFile(ProgrammingLanguage lang, int i) {
		int count=0;
		for (SourceFile sf : getSourceFiles(lang)) {
			if (sf.isEditable())
				if (i == count)
					return sf;
				count++;
		}
		throw new ArrayIndexOutOfBoundsException("Not "+i+" public source files (but only "+count+")");
	}
	public SourceFile getPublicSourceFile(ProgrammingLanguage lang, String name) {
		for (SourceFile sf : getSourceFiles(lang)) {
			if (sf.getName().equals(name))
				return sf;
		}
		return null; // not found
	}
	
	public int worldCount() {
		return this.initialWorld.length;
	}
	
	/** Returns the current world number index 
	 * @see #getAnswerOfWorld(int)
	 */
	public World getWorld(int index) {
		return this.currentWorld[index];
	}
	
	public int indexOfWorld(World w) {
		int index = 0;
		do {
			if (this.currentWorld[index] == w)
				return index;
			index++;
		} while (index < this.currentWorld.length);
		
		throw new RuntimeException("World not found (please report this bug)");
	}
	
	public World getAnswerOfWorld(int index) {
		return this.answerWorld[index];
	}
	
	public String getTip(String tipsId) {
		return this.tips.get(tipsId);
	}
	public String toString() {
		return name;
	}

	/* setters and getter of the programming language that this exercise accepts */ 
	private ProgrammingLanguage[] progLanguages = new ProgrammingLanguage[] {Game.JAVA};
	public ProgrammingLanguage[] getProgLanguages() {
		return progLanguages;
	}
	public void addProgLanguage(ProgrammingLanguage newL) {
		ProgrammingLanguage[] res = new ProgrammingLanguage[progLanguages.length +1 ];
		for (int i=0;i<progLanguages.length;i++) 
			res[i] = progLanguages[i];
		res[progLanguages.length] = newL;
		progLanguages = res;
	}
	public void addProgLanguage(ProgrammingLanguage[] newL) { /* FIXME: inefficient: increase the array size only once */
		for (ProgrammingLanguage l:newL)
			addProgLanguage(l);
	}
	public void setProgLanguages(ProgrammingLanguage ... languages) {
		progLanguages = languages;
	}
}

