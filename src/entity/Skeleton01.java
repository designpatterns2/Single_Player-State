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

package entity;

import jglcore.JGL_3DVector;
import jglcore.JGL_Math;
import jglcore.JGL_Time;
import phys.Motion;
import phys.Mover;
import phys.Mover_gravity;
import phys.Mover_none;
import phys.Shape;
import phys.Trace;
import world.World;
import ai.SkeletonAI;

import com.jme.math.Quaternion;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;
import com.jmex.model.animation.KeyframeController;


/**
 * Class representing the skeletons entities.
 * 
 * @author Nicolas Devere
 *
 */
public final class Skeleton01 implements Entity, Scriptable {
	
	private String name;
	private int state;
	
	private int team;
	private float lifeMax;
	private float life;
	private float dam;
	private float sp;
	
	private static long DYING_PERIOD = 0l;
	private long dyingCumulTime;
	
	private Node node;
	private KeyframeController kc;
	private float kspeed;
	private int nbFrames;
	private float t;
	private int minKey;
	private int maxKey;
	private int length;
	
	private Blood[] bloods;
	private int bloodIndex;
	
	private Explosion expNode;
	
	private boolean collidable;
	private Mover cmover;
	private Motion cmotion;
	private Shape cshape;
	
	private JGL_3DVector angles;
	private Quaternion qy;
	
	private JGL_3DVector way;
	private int forward;
	private int asides;
	private float diag;
	
	private Weapon weapon;
	private SkeletonAI ai;
	
	private ScriptBox scriptbox;
	
	
	public Skeleton01(	String id, int team, float posX, float posY, float posZ, 
						float angleX, float angleY, float angleZ, 
						float maxLife, float damage, float speed, 
						Node protoNode, Blood[] bloods, Explosion explode, 
						Shape shape, Mover mover, Motion motion) {
		
		name = id;
		state = ACTIVE;

		this.team = team;
		life = lifeMax = maxLife;
		dam = damage;
		sp = speed;

		dyingCumulTime = 0l;

		node = protoNode;
		if (protoNode.getChild(0).getControllerCount()>0) {
			kc = (KeyframeController)protoNode.getChild(0).getController(0);
			kspeed = kc.getSpeed() - (kc.getSpeed() * (0.2f - (0.2f*JGL_Math.rnd()) ));
			nbFrames = kc.keyframes.size();
		}
		else {
			kc = null;
			kspeed = 0f;
			nbFrames = 1;
		}
		setAnimationFrames(0, nbFrames - 1);

		this.bloods = bloods;
		bloodIndex = 0;
		
		expNode = explode;
		
		collidable = true;
		//cmover = mover;
		cmover = new Mover_gravity(new JGL_3DVector(0f, 0f, -1f));
		cmover.setSpeed(sp);
		cshape = shape;
		cshape.setPosition(new JGL_3DVector(posX, posY, posZ));
		cmotion = motion;

		angles = new JGL_3DVector(angleX, angleY, angleZ);
		qy = new Quaternion();
		way = new JGL_3DVector();
		forward = 0;
		asides = 0;
		diag = JGL_Math.cos(45f);
		
		weapon = null;
		ai = null;
		scriptbox = null;
	}
	
	
	/**
	 * Sets the min and max keyframes for the animation to play.
	 * 
	 * @param minKeyframe : the min keyframe
	 * @param maxKeyframe : the max keyframe
	 */
	public void setAnimationFrames(int minKeyframe, int maxKeyframe) {
		if (maxKeyframe<minKeyframe) return;
		if (minKeyframe<0) minKeyframe = 0;
		if (maxKeyframe<0) maxKeyframe = 0;
		if (minKeyframe>nbFrames - 1) minKeyframe = nbFrames - 1;
		if (maxKeyframe>nbFrames - 1) maxKeyframe = nbFrames - 1;
		
		minKey = minKeyframe;
		maxKey = maxKeyframe;
		length = (maxKey - minKey);
		
		t = minKey;
	
	}
	
	
	public void reset() {
		state = ACTIVE;
		life = lifeMax;
		bloodIndex = 0;
		collidable = true;
		cmover.setSpeed(sp);
		setForwardMove(0);
		setSideMove(0);
		if (scriptbox!=null)
			scriptbox.reset();
	}
	
	@Override
	public Shape getCShape() {
		// TODO Auto-generated method stub
		return cshape;
	}

	@Override
	public Motion getCollider() {
		// TODO Auto-generated method stub
		return cmotion;
	}

	@Override
	public float getDamage() {
		// TODO Auto-generated method stub
		return dam * JGL_Time.getTimer();
	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public float getLife() {
		// TODO Auto-generated method stub
		return life;
	}

	@Override
	public Mover getMover() {
		// TODO Auto-generated method stub
		return cmover;
	}

	@Override
	public Node getNode() {
		// TODO Auto-generated method stub
		return node;
	}

	@Override
	public JGL_3DVector getOrientation() {
		// TODO Auto-generated method stub
		return angles;
	}

	@Override
	public JGL_3DVector getPosition() {
		// TODO Auto-generated method stub
		return cshape.getPosition();
	}

	@Override
	public int getTeam() {
		// TODO Auto-generated method stub
		return team;
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return state == ACTIVE;
	}

	@Override
	public boolean isCollidable() {
		// TODO Auto-generated method stub
		return collidable;
	}

	@Override
	public boolean isDead() {
		// TODO Auto-generated method stub
		return state == DEAD;
	}

	@Override
	public boolean isDying() {
		// TODO Auto-generated method stub
		return state == DYING;
	}

	@Override
	public void render() {
		// TODO Auto-generated method stub
		if (kc!=null)
			kc.setCurTime(t);
		node.setLocalTranslation(getPosition().x, getPosition().y, getPosition().z);
		
		//	 Y axis Quaternion compute
		float angle = angles.y * 0.5f;
		qy.set(0f, -JGL_Math.sin(angle), 0f, JGL_Math.cos(angle));
		
		node.setLocalRotation(qy);
		node.updateGeometricState(0f, true);
		DisplaySystem.getDisplaySystem().getRenderer().draw(node);
	}

	@Override
	public void setActive() {
		// TODO Auto-generated method stub
		state = ACTIVE;
	}

	@Override
	public void setCShape(Shape arg) {
		// TODO Auto-generated method stub
		cshape = arg;
	}

	@Override
	public void setCollidable(boolean arg) {
		// TODO Auto-generated method stub
		collidable = arg;
	}

	@Override
	public void setCollider(Motion arg) {
		// TODO Auto-generated method stub
		cmotion = arg;
	}

	@Override
	public void setDamage(float arg) {
		// TODO Auto-generated method stub
		dam = arg;
	}

	@Override
	public void setDead() {
		// TODO Auto-generated method stub
		state = DEAD;
		expNode.reset(getPosition().x, getPosition().y, getPosition().z);
		World.map.addObject(expNode);
		executeScripts();
	}

	@Override
	public void setDying() {
		// TODO Auto-generated method stub
		setCollidable(false);
		setAnimationFrames(nbFrames - 1, nbFrames - 1);
		dyingCumulTime = 0l;
		cmover = new Mover_none();
		setForwardMove(0);
		setSideMove(0);
		ai = null;
		state = DYING;
	}

	@Override
	public void setLife(float arg) {
		// TODO Auto-generated method stub
		if (arg>lifeMax)
			arg = lifeMax;
		life = arg;
	}

	@Override
	public void setMover(Mover arg) {
		// TODO Auto-generated method stub
		//cmover = arg;
	}

	@Override
	public void setSpeed(float arg) {
		// TODO Auto-generated method stub
		sp = arg;
		cmover.setSpeed(arg);
	}

	@Override
	public void setTeam(int arg) {
		// TODO Auto-generated method stub
		team = arg;
	}
	
	
	/**
	 * Affiliates the specified weapon to this Human.
	 * 
	 * @param _weapon : the weapon to affiliate to this Human
	 */
	public void linkWeapon(Weapon _weapon) {
		weapon = _weapon;
		weapon.setOwner(this);
	}
	
	
	/**
	 * Returns the linked weapon, or null otherwise.
	 * 
	 * @return the linked weapon, or null otherwise
	 */
	public Weapon getWeapon() {
		return weapon;
	}
	
	
	/**
	 * Makes the entity shoot if a weapon is linked, nothing otherwise.
	 */
	public void shoot() {
		if (weapon!=null && state==ACTIVE)
			weapon.shoot(getPosition());
	}
	
	
	/**
	 * Affiliates a AI object to this skeleton.
	 * 
	 * @param SkeletonAI : the AI object to affiliate to this skeleton
	 */
	public void linkAI(SkeletonAI zombieAI) {
		ai = zombieAI;
	}
	

	@Override
	public void synchronizeNode() {
		// TODO Auto-generated method stub
		if (length>0) {
			t += JGL_Time.getTimePerFrame() * kspeed;
			while (t >= maxKey)
				t -= length;
		}
	}

	@Override
	public boolean touchReact(Entity entity, Trace trace) {
		// TODO Auto-generated method stub
		if (entity.getDamage()>0f && trace.isImpact()) {
			life -= entity.getDamage();
			float x = trace.start.x + (trace.segment.x * trace.fractionImpact);
			float y = trace.start.y + (trace.segment.y * trace.fractionImpact);
			float z = trace.start.z + (trace.segment.z * trace.fractionImpact);
			bloods[bloodIndex].reset(new JGL_3DVector(x, y, z));
			World.map.addObject(bloods[bloodIndex]);
			bloodIndex++;
			if (bloodIndex>=bloods.length)
				bloodIndex = 0;
		}
		
		if (life<=0f)
			setDying();
		return true;
	}
	
	
	/**
	 * Go forward/backward.
	 */
	public void setForwardMove(int arg) {
		if (isActive()) {
			forward = arg;
			setMove();
		}
	}
	
	
	/**
	 * Go left/right.
	 */
	public void setSideMove(int arg) {
		if (isActive()) {
			asides = arg;
			setMove();
		}
	}
	
	
	// Updates the base direction
	private void setMove() {

		if (forward==1) {
			if (asides==1) {
				way.x = diag;
				way.z = -diag;
			}
			else if (asides==0) {
				way.x = 0f;
				way.z = -1f;
			}
			else {
				way.x = -diag;
				way.z = -diag;
			}
			
		}
		else if (forward==0) {
			if (asides==1) {
				way.x = 1f;
				way.z = 0f;
			}
			else if (asides==0) {
				way.x = 0f;
				way.z = 0f;
			}
			else {
				way.x = -1f;
				way.z = 0f;
			}
		}
		else {
			if (asides==1) {
				way.x = diag;
				way.z = diag;
			}
			else if (asides==0) {
				way.x = 0f;
				way.z = 1f;
			}
			else {
				way.x = -diag;
				way.z = diag;
			}
		}
	}
	
	
	/**
	 * Increase the entity rotation on X and Y axis.
	 * 
	 * @param deltaX : the X angle to add
	 * @param deltaY : the Y angle to add
	 */
	public void increaseAngles(float deltaX, float deltaY) {
		
		angles.x += deltaX;
		angles.y += deltaY;
		
		if (angles.x>90f) angles.x = 90f;
		if (angles.x<-90f) angles.x = -90f;
		while (angles.y>360f) angles.y -= 360f;
		while (angles.y<-360f) angles.y += 360f;
	}
	

	@Override
	public void update() {
		// TODO Auto-generated method stub
		if (isActive()) {
			ai.update();
			JGL_Math.vector_fastYXrotate(way, 0f, angles.y, cmover.getVelocity());
			cmover.update();
		}
		
		else if (isDying()) {
			dyingCumulTime += JGL_Time.getTimePerFrame() * 1000f;
			if (dyingCumulTime>=DYING_PERIOD)
				setDead();
		}
	}

	@Override
	public void executeScripts() {
		// TODO Auto-generated method stub
		if (scriptbox!=null)
			scriptbox.executeScripts();
	}

	@Override
	public void storeScriptBox(ScriptBox arg) {
		// TODO Auto-generated method stub
		scriptbox = arg;
	}
	
	
	public Object clone() {
		Blood bs[] = new Blood[bloods.length];
	    for (int i=0; i<bloods.length; i++)
	    	bs[i] = (Blood)bloods[i].clone();
	    return new Skeleton01(name, team, getPosition().x, getPosition().y, getPosition().z, 
	    					angles.x, angles.y, angles.z, lifeMax, dam, sp, node, bs, (Explosion)expNode.clone(), 
	    					(Shape)cshape.clone(), (Mover)cmover.clone(), (Motion)cmotion.clone());
	}

}
