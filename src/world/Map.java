//	Copyright 2009 Nicolas Devere
//
//	This file is part of FLESH SNATCHER.
//
//	FLESH SNATCHER is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//
//	FLESH SNATCHER is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//
//	You should have received a copy of the GNU General Public License
//	along with FLESH SNATCHER; if not, write to the Free Software
//	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

package world;

import java.util.Vector;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import entity.Entity;
import entity.Scriptable;
import entity.Shoot;
import phys.Tracable;
import phys.Trace;
import jglcore.JGL_3DVector;
import jglcore.JGL_3DPlane;
import jglcore.JGL_Time;


/**
 * Class representing a game map. It stores a land and manages entities.
 * 
 * @author Nicolas Devere
 *
 */
public class Map implements Tracable {
	
	public static float MIN_HEIGHT = -350f;
	
	public Node sky;
	
	public Vector displayNodes;
	public Vector collNodes;
	public Vector characters;
	public Vector shoots;
	public Vector objects;
	public Vector scripts;
	
	private Scriptable checkpoint;
	
	private static int SHOOT = 0;
	private static int CHAR = 1;
	private static int OBJ = 2;
	private int entType;
	private Entity ent;
	
	private boolean finished;
	
	
	
	public Map() {
		
		sky = null;
		
		displayNodes = new Vector();
		collNodes = new Vector();
		characters = new Vector();
		shoots = new Vector();
		objects = new Vector();
		scripts = new Vector();
		
		finished = false;
	}
	
	
	public void setSky(Node arg) {
		sky = arg;
	}
	
	public void addDisplayNode(DisplayNode node) {
		if (!displayNodes.contains(node))
			displayNodes.add(node);
	}
	
	public void removeDisplayNode(DisplayNode node) {
		displayNodes.remove(node);
	}
	
	public void addCollisionNode(CollisionNode node) {
		if (!collNodes.contains(node)) {
			node.setInGame(true);
			collNodes.add(node);
		}
	}
	
	public void removeCollisionNode(CollisionNode node) {
		if (collNodes.contains(node)) {
			node.setInGame(false);
			collNodes.remove(node);
		}
	}
	
	public void addCharacter(Entity character) {
		if (!characters.contains(character))
			characters.add(character);
	}
	
	public void addShoot(Entity shoot) {
		if (!shoots.contains(shoot))
			shoots.add(shoot);
	}
	
	public void addObject(Entity object) {
		if (!objects.contains(object))
			objects.add(object);
	}
	
	public void addScriptBox(Entity scriptbox) {
		if (!scripts.contains(scriptbox))
			scripts.add(scriptbox);
	}
	
	public void setCheckpoint(Scriptable scriptable) {
		checkpoint = scriptable;
	}
	
	public void applyCheckpoint() {
		if (checkpoint!=null)
			checkpoint.executeScripts();
	}
	
	
	public DisplayNode getDisplayNode(String name) {
		DisplayNode n;
		for (int i=0; i<displayNodes.size(); i++) {
			n = (DisplayNode)displayNodes.get(i);
			if (n.getName().equals(name))
				return n;
		}
		return null;
	}
	
	
	public void update() {
		
		Entity c;
		//Shoot s;
		int i;
		boolean collision = false;
		
		// Update shoots
		entType = SHOOT;
		for (i=0; i<shoots.size(); i++) {
			c = (Shoot)shoots.get(i);
			c.update();
			ent = c;
			collision = c.getCollider().process(c.getCShape(), c.getMover(), this);
			c.synchronizeNode();
			if (collision)
				c.setDead();
			if (c.isDead()) {
				shoots.remove(i);
				i--;
			}
		}
		
		// Update characters
		entType = CHAR;
		for (i=0; i<characters.size(); i++) {
			c = (Entity)characters.get(i);
			c.update();
			ent = c;
			c.getCollider().process(c.getCShape(), c.getMover(), this);
			c.synchronizeNode();
			if (c.getPosition().y<MIN_HEIGHT)
				c.setDead();
			if (c.isDead()) {
				characters.remove(i);
				i--;
			}
		}
		
		// Update objects
		entType = OBJ;
		for (i=0; i<objects.size(); i++) {
			c = (Entity)objects.get(i);
			c.update();
			ent = c;
			c.getCollider().process(c.getCShape(), c.getMover(), this);
			c.synchronizeNode();
			if (c.isDead()) {
				objects.remove(i);
				i--;
			}
		}
		
		// Update scriptboxes
		for (i=0; i<scripts.size(); i++) {
			c = (Entity)scripts.get(i);
			//c.update();
			//ent = c;
			//c.getCollider().process(c.getCShape(), c.getMover(), this);
			//c.synchronizeNode();
			if (c.isDead()) {
				scripts.remove(i);
				i--;
			}
		}
		
	}
	
	
	public void setFinished() {
		finished = true;
	}
	
	
	public boolean isFinished() {
		return finished;
	}
	

	@Override
	public boolean trace(Trace trace) {
		// TODO Auto-generated method stub
		
		int i;
		Entity e;
		Entity target = null;
		
		if (!ent.isCollidable() && !ent.isActive())
			return false;
		
		// Impact on collision Nodes
		boolean is_impact = traceScenery(trace);
		
		// Impact on characters
		for (i=0; i<characters.size(); i++) {
			e = (Entity)characters.get(i);
			if (e != ent  && e.isCollidable() && e.isActive() && ( 	(entType==SHOOT && e.getTeam() != ent.getTeam()) || 
																	(entType!=SHOOT) ) )
				if (e.getCShape().trace(trace)) {
					target = e;
					is_impact = true;
				}
		}
		
		// Impact on objects
		for (i=0; i<objects.size(); i++) {
			e = (Entity)objects.get(i);
			if (e != ent && e.isCollidable() && e.isActive())
				if (e.getCShape().trace(trace)) {
					target = e;
					is_impact = true;
				}
		}
		
		if (target!=null)
			if (target.getTeam()!=ent.getTeam())
				target.touchReact(ent, trace);
		
		// Impact on scriptboxes
		float fi = trace.fractionImpact;
		float fr = trace.fractionReal;
		JGL_3DPlane n = trace.correction;
		boolean scriptImpact = false;
		for (i=0; i<scripts.size(); i++) {
			e = (Entity)scripts.get(i);
			if (e.getCShape().trace(trace)) {
				target = e;
				scriptImpact = true;
			}
		}
		
		if (scriptImpact) {
			trace.setImpact(n, fi, fr);
			target.touchReact(ent, trace);
		}
		
		return is_impact;
	}
	
	
	
	public boolean intersect(JGL_3DVector p1, JGL_3DVector p2) {
		
		for (int i=0; i<collNodes.size(); i++)
			if (((CollisionNode)collNodes.get(i)).intersect(p1, p2))
				return true;
		
		return false;
	}
	
	
	public boolean traceScenery(Trace trace) {
		
		int i;
		CollisionNode cn;
		CollisionNode cNode = null;
		boolean is_impact = false;
		
		// Impact on collision Nodes
		for (i=0; i<collNodes.size() && cNode==null; i++) {
			cn = (CollisionNode)collNodes.get(i);
			if (cn.isIn(trace))
				cNode = cn;
		}
		if (cNode!=null)
			is_impact = cNode.collideRecursive(trace);
		else
			for (i=0; i<collNodes.size(); i++)
				is_impact |= ((CollisionNode)collNodes.get(i)).collideSimple(trace, false);
		return is_impact;
	}
	
	
	public void render(JGL_3DVector eye) {
		
		if (sky!=null) {
			sky.setLocalTranslation(eye.x, eye.y, eye.z);
			sky.updateGeometricState(JGL_Time.getTimePerFrame(), true);
			DisplaySystem.getDisplaySystem().getRenderer().draw(sky);
		}
		
		for (int i=0; i<displayNodes.size(); i++)
			((DisplayNode)displayNodes.get(i)).render(eye);
		
		for (int i=0; i<objects.size(); i++)
			((Entity)objects.get(i)).render();
		
		for (int i=0; i<characters.size(); i++)
			((Entity)characters.get(i)).render();
		
		for (int i=0; i<shoots.size(); i++)
			((Entity)shoots.get(i)).render();
	}
	
	
	public void clear() {
		displayNodes.clear();
		collNodes.clear();
		characters.clear();
		shoots.clear();
		objects.clear();
		scripts.clear();
		Runtime.getRuntime().gc();
	}
	
}
