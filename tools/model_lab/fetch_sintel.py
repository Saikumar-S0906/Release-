from remotezip import RemoteZip
import os,re
z=RemoteZip("http://files.is.tue.mpg.de/sintel/MPI-Sintel-complete.zip")
n=z.namelist()
flows=sorted(x for x in n if x.startswith('training/flow/') and x.endswith('.flo'))
sel=[f for i,f in enumerate(flows) if i%10==0][:104]
out="/app/lab/data/sintel"
for f in sel:
    sc,fr=f.split('/')[2],f.split('/')[3]
    k=int(re.findall(r'\d+',fr)[0])
    for p in [f,f"training/clean/{sc}/frame_{k:04d}.png",f"training/clean/{sc}/frame_{k+1:04d}.png"]:
        z.extract(p,out)
print("SINTEL_DONE",len(sel))
